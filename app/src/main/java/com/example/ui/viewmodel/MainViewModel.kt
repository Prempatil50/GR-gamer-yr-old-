package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiReplyEngine
import com.example.data.firebase.FirestoreRepository
import com.example.data.model.AiAnalysisResult
import com.example.data.model.AiSettings
import com.example.data.model.ChannelConfig
import com.example.data.model.CommentRecord
import com.example.data.model.CommentStatus
import com.example.data.model.LanguageMode
import com.example.data.model.ReplyHistoryItem
import com.example.data.model.ReplyStatus
import com.example.data.model.ReplyTone
import com.example.data.model.UiState
import com.example.data.model.YouTubeChannelOption
import com.example.data.youtube.YouTubeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class EngineRuntimeState(
    val accessToken: String? = null,
    val isConnectingChannel: Boolean = false,
    val isSyncingComments: Boolean = false,
    val isProcessingReplies: Boolean = false,
    val processingCommentId: String? = null,
    val discoveredChannels: List<YouTubeChannelOption> = emptyList(),
    val showChannelPicker: Boolean = false,
    val statusBanner: String? = null,
    val errorBanner: String? = null,
    val channelError: String? = null,
    val channelNeedsCloudSetup: Boolean = false,
    val lastSyncLabel: String = "Not synced yet",
    val isTestingAi: Boolean = false,
    val aiTestResult: AiAnalysisResult? = null,
    val aiTestError: String? = null
)

class MainViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val youtubeRepo: YouTubeRepository,
    private val geminiEngine: GeminiReplyEngine,
    val currentUserId: String
) : ViewModel() {

    private val _runtime = MutableStateFlow(EngineRuntimeState())
    val runtime: StateFlow<EngineRuntimeState> = _runtime.asStateFlow()

    val channelState: StateFlow<UiState<ChannelConfig>> = firestoreRepo
        .observeChannelConfig(currentUserId)
        .map<ChannelConfig, UiState<ChannelConfig>> { UiState.Success(it) }
        .catch { error ->
            val fsError = unwrapFirestoreError(error)
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).localizedMessage ?: "Failed to load channel state"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val settingsState: StateFlow<UiState<AiSettings>> = firestoreRepo
        .observeAiSettings(currentUserId)
        .map<AiSettings, UiState<AiSettings>> { UiState.Success(it) }
        .catch { error ->
            val fsError = unwrapFirestoreError(error)
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).localizedMessage ?: "Failed to load AI settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val commentsState: StateFlow<UiState<List<CommentRecord>>> = firestoreRepo
        .observeComments(currentUserId)
        .map<List<CommentRecord>, UiState<List<CommentRecord>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = unwrapFirestoreError(error)
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).localizedMessage ?: "Failed to load comments"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val historyState: StateFlow<UiState<List<ReplyHistoryItem>>> = firestoreRepo
        .observeReplyHistory(currentUserId)
        .map<List<ReplyHistoryItem>, UiState<List<ReplyHistoryItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = unwrapFirestoreError(error)
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).localizedMessage ?: "Failed to load reply history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private var autoReplyLoopJob: Job? = null

    init {
        viewModelScope.launch {
            firestoreRepo.ensureDefaultAiSettings(currentUserId)
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    fun clearBanners() {
        _runtime.update { it.copy(statusBanner = null, errorBanner = null) }
    }

    fun setConnectingChannel(connecting: Boolean) {
        _runtime.update {
            it.copy(
                isConnectingChannel = connecting,
                errorBanner = null,
                channelError = null,
                channelNeedsCloudSetup = false
            )
        }
    }

    fun reportError(message: String) {
        _runtime.update {
            it.copy(
                isConnectingChannel = false,
                isSyncingComments = false,
                isProcessingReplies = false,
                errorBanner = message
            )
        }
    }

    fun reportChannelError(message: String, needsCloudSetup: Boolean = false) {
        val cloudFlag = needsCloudSetup ||
            message.contains("403", ignoreCase = true) ||
            message.contains("YouTube Data API v3", ignoreCase = true) ||
            message.contains("DEVELOPER_ERROR", ignoreCase = true) ||
            message.contains("Test User", ignoreCase = true) ||
            message.contains("Cloud Console", ignoreCase = true)
        _runtime.update {
            it.copy(
                isConnectingChannel = false,
                channelError = message,
                channelNeedsCloudSetup = cloudFlag,
                errorBanner = message
            )
        }
    }

    fun onOAuthTokenAcquired(token: String, autoPickSingleChannel: Boolean = true) {
        viewModelScope.launch {
            _runtime.update {
                it.copy(
                    accessToken = token,
                    isConnectingChannel = true,
                    errorBanner = null,
                    channelError = null,
                    channelNeedsCloudSetup = false
                )
            }
            val result = youtubeRepo.fetchMyChannels(token)
            result.onSuccess { channels ->
                if (channels.isEmpty()) {
                    reportChannelError(
                        message = "No YouTube channel was found on this Google account. If your YouTube channel belongs to a different account or Brand Account, tap 'Connect with Google' or 'Google Login (Email & Password)'."
                    )
                } else if (channels.size == 1 && autoPickSingleChannel) {
                    connectSelectedChannel(channels.first())
                } else {
                    _runtime.update {
                        it.copy(
                            isConnectingChannel = false,
                            discoveredChannels = channels,
                            showChannelPicker = true
                        )
                    }
                }
            }.onFailure { err ->
                reportChannelError(
                    message = err.localizedMessage ?: "Failed to load YouTube channels."
                )
            }
        }
    }

    fun openChannelPickerWithCurrentToken() {
        val token = _runtime.value.accessToken
        if (token.isNullOrBlank()) {
            reportChannelError("Tap Connect YouTube Channel to authorize Google OAuth 2.0.")
            return
        }
        onOAuthTokenAcquired(token, autoPickSingleChannel = false)
    }

    fun dismissChannelPicker() {
        _runtime.update { it.copy(showChannelPicker = false) }
    }

    fun connectSelectedChannel(channel: YouTubeChannelOption) {
        viewModelScope.launch {
            _runtime.update {
                it.copy(
                    isConnectingChannel = true,
                    showChannelPicker = false,
                    channelError = null,
                    channelNeedsCloudSetup = false
                )
            }
            firestoreRepo.saveConnectedChannel(currentUserId, channel)
                .onSuccess {
                    _runtime.update {
                        it.copy(
                            isConnectingChannel = false,
                            statusBanner = "Connected to ${channel.title} (${channel.handle})"
                        )
                    }
                    // Immediately sync comments once connected
                    syncCommentsFromYouTube(autoReplyAfterSync = false)
                }
                .onFailure { err ->
                    reportChannelError(
                        message = err.localizedMessage ?: "Failed to save channel."
                    )
                }
        }
    }

    fun disconnectYouTubeChannel() {
        viewModelScope.launch {
            stopAutoReplyEngineInternal()
            firestoreRepo.disconnectChannel(currentUserId)
                .onSuccess {
                    _runtime.update {
                        it.copy(
                            accessToken = null,
                            discoveredChannels = emptyList(),
                            showChannelPicker = false,
                            statusBanner = "YouTube channel disconnected safely."
                        )
                    }
                }
                .onFailure { err ->
                    reportError(err.localizedMessage ?: "Failed to disconnect YouTube channel.")
                }
        }
    }

    fun startAutoReply() {
        val channel = (channelState.value as? UiState.Success)?.data
        if (channel == null || !channel.connected || channel.channelId.isBlank()) {
            reportError("Connect your YouTube channel first before starting Auto Reply.")
            return
        }
        if (_runtime.value.accessToken.isNullOrBlank()) {
            reportError("YouTube OAuth session expired or not authorized yet. Tap Re-Authorize YouTube in Channel Connect.")
            return
        }
        viewModelScope.launch {
            firestoreRepo.setAutoReplyActive(currentUserId, true)
                .onSuccess {
                    _runtime.update { it.copy(statusBanner = "GR GAMER AI Auto-Reply Engine is LIVE!") }
                    startAutoReplyLoop()
                }
                .onFailure { err ->
                    reportError(err.localizedMessage ?: "Unable to start Auto Reply.")
                }
        }
    }

    fun stopAutoReply() {
        viewModelScope.launch {
            stopAutoReplyEngineInternal()
            firestoreRepo.setAutoReplyActive(currentUserId, false)
                .onSuccess {
                    _runtime.update { it.copy(statusBanner = "Auto-Reply Engine paused.") }
                }
                .onFailure { err ->
                    reportError(err.localizedMessage ?: "Unable to stop Auto Reply.")
                }
        }
    }

    fun toggleAutoReply(enabled: Boolean) {
        if (enabled) startAutoReply() else stopAutoReply()
    }

    private fun stopAutoReplyEngineInternal() {
        autoReplyLoopJob?.cancel()
        autoReplyLoopJob = null
    }

    private fun startAutoReplyLoop() {
        autoReplyLoopJob?.cancel()
        autoReplyLoopJob = viewModelScope.launch {
            while (isActive) {
                val settings = (settingsState.value as? UiState.Success)?.data ?: AiSettings(userId = currentUserId)
                val channel = (channelState.value as? UiState.Success)?.data
                val token = _runtime.value.accessToken

                if (channel == null || !channel.connected || token.isNullOrBlank()) {
                    firestoreRepo.setAutoReplyActive(currentUserId, false)
                    _runtime.update {
                        it.copy(errorBanner = "Auto-Reply paused: YouTube authorization required.")
                    }
                    break
                }

                runSyncAndProcessCycle(autoReply = true)
                val intervalMs = (settings.pollingIntervalSeconds.coerceIn(15L, 300L)) * 1000L
                delay(intervalMs)
            }
        }
    }

    fun syncCommentsFromYouTube(autoReplyAfterSync: Boolean = false) {
        viewModelScope.launch {
            runSyncAndProcessCycle(autoReply = autoReplyAfterSync)
        }
    }

    private suspend fun runSyncAndProcessCycle(autoReply: Boolean) {
        val channel = (channelState.value as? UiState.Success)?.data
        val token = _runtime.value.accessToken
        if (channel == null || !channel.connected || channel.channelId.isBlank()) {
            reportError("No YouTube channel connected.")
            return
        }
        if (token.isNullOrBlank()) {
            reportError("YouTube OAuth authorization required. Open YouTube Channel Connect to authorize.")
            return
        }

        _runtime.update { it.copy(isSyncingComments = true, errorBanner = null) }
        val fetchResult = youtubeRepo.fetchLatestComments(token, currentUserId, channel.channelId)
        fetchResult.onSuccess { fetchedList ->
            fetchedList.forEach { item ->
                firestoreRepo.upsertFetchedComment(currentUserId, item.record)
            }
            _runtime.update {
                it.copy(
                    isSyncingComments = false,
                    lastSyncLabel = "Synced ${fetchedList.size} threads • ${currentTimeShort()}",
                    statusBanner = if (!autoReply) "Fetched ${fetchedList.size} latest YouTube comments." else it.statusBanner
                )
            }
            recomputeAndSyncStats()

            if (autoReply) {
                processPendingCommentsWithAi(token)
            }
        }.onFailure { err ->
            _runtime.update {
                it.copy(
                    isSyncingComments = false,
                    errorBanner = err.localizedMessage ?: "Failed to fetch YouTube comments."
                )
            }
        }
    }

    private suspend fun processPendingCommentsWithAi(accessToken: String) {
        val settings = (settingsState.value as? UiState.Success)?.data ?: AiSettings(userId = currentUserId)
        val comments = (commentsState.value as? UiState.Success)?.data.orEmpty()
        val pending = comments.filter { it.status == CommentStatus.PENDING.name }
        if (pending.isEmpty()) return

        _runtime.update { it.copy(isProcessingReplies = true) }
        val recentReplies = (historyState.value as? UiState.Success)?.data
            .orEmpty()
            .filter { it.status == ReplyStatus.SENT.name }
            .map { it.aiReply }
            .toMutableList()

        for (comment in pending) {
            if (settings.avoidDuplicates && firestoreRepo.hasAlreadyReplied(currentUserId, comment.youtubeCommentId)) {
                firestoreRepo.updateCommentStatus(
                    userId = currentUserId,
                    youtubeCommentId = comment.youtubeCommentId,
                    detectedLanguage = comment.detectedLanguage,
                    status = CommentStatus.REPLIED,
                    generatedReply = comment.generatedReply,
                    failureReason = "",
                    repliedAtIso = comment.repliedAtIso
                )
                continue
            }
            processSingleCommentInternal(
                accessToken = accessToken,
                comment = comment,
                settings = settings,
                recentReplies = recentReplies
            )
        }
        _runtime.update { it.copy(isProcessingReplies = false, processingCommentId = null) }
        recomputeAndSyncStats()
    }

    fun replyToSingleComment(comment: CommentRecord) {
        val token = _runtime.value.accessToken
        if (token.isNullOrBlank()) {
            reportError("Authorize YouTube OAuth in Channel Connect before posting replies.")
            return
        }
        viewModelScope.launch {
            _runtime.update { it.copy(isProcessingReplies = true, processingCommentId = comment.youtubeCommentId) }
            val settings = (settingsState.value as? UiState.Success)?.data ?: AiSettings(userId = currentUserId)
            val recentReplies = (historyState.value as? UiState.Success)?.data
                .orEmpty()
                .filter { it.status == ReplyStatus.SENT.name }
                .map { it.aiReply }
                .toMutableList()
            processSingleCommentInternal(token, comment, settings, recentReplies)
            _runtime.update { it.copy(isProcessingReplies = false, processingCommentId = null) }
            recomputeAndSyncStats()
        }
    }

    private suspend fun processSingleCommentInternal(
        accessToken: String,
        comment: CommentRecord,
        settings: AiSettings,
        recentReplies: MutableList<String>
    ) {
        _runtime.update { it.copy(processingCommentId = comment.youtubeCommentId) }
        val analysisResult = geminiEngine.analyzeAndGenerateReply(
            commentText = comment.commentText,
            authorName = comment.authorName,
            videoTitle = comment.videoTitle,
            settings = settings,
            recentReplies = recentReplies
        )

        analysisResult.onSuccess { analysis ->
            if (analysis.classification == "SPAM" && settings.ignoreSpam) {
                firestoreRepo.updateCommentStatus(
                    userId = currentUserId,
                    youtubeCommentId = comment.youtubeCommentId,
                    detectedLanguage = analysis.detectedLanguage,
                    status = CommentStatus.SKIPPED_SPAM,
                    generatedReply = "",
                    failureReason = analysis.reason.ifBlank { "Filtered as spam" },
                    repliedAtIso = ""
                )
                firestoreRepo.recordReplyHistory(
                    userId = currentUserId,
                    item = ReplyHistoryItem(
                        userId = currentUserId,
                        youtubeCommentId = comment.youtubeCommentId,
                        videoId = comment.videoId,
                        videoTitle = comment.videoTitle,
                        authorName = comment.authorName,
                        originalComment = comment.commentText,
                        aiReply = "Skipped (Spam filtered: ${analysis.reason})",
                        languageUsed = analysis.detectedLanguage,
                        status = ReplyStatus.SKIPPED.name,
                        errorMessage = "",
                        postedToYoutube = false
                    )
                )
                return
            }

            if (analysis.classification == "ABUSIVE" && settings.ignoreAbusive) {
                firestoreRepo.updateCommentStatus(
                    userId = currentUserId,
                    youtubeCommentId = comment.youtubeCommentId,
                    detectedLanguage = analysis.detectedLanguage,
                    status = CommentStatus.SKIPPED_ABUSIVE,
                    generatedReply = "",
                    failureReason = analysis.reason.ifBlank { "Filtered as abusive" },
                    repliedAtIso = ""
                )
                firestoreRepo.recordReplyHistory(
                    userId = currentUserId,
                    item = ReplyHistoryItem(
                        userId = currentUserId,
                        youtubeCommentId = comment.youtubeCommentId,
                        videoId = comment.videoId,
                        videoTitle = comment.videoTitle,
                        authorName = comment.authorName,
                        originalComment = comment.commentText,
                        aiReply = "Skipped (Abusive comment filtered)",
                        languageUsed = analysis.detectedLanguage,
                        status = ReplyStatus.SKIPPED.name,
                        errorMessage = "",
                        postedToYoutube = false
                    )
                )
                return
            }

            val replyText = analysis.replyText.ifBlank {
                "Thanks for watching and supporting GR GAMER! Stay tuned for more Free Fire action!"
            }

            val postResult = youtubeRepo.postReplyToComment(
                accessToken = accessToken,
                parentCommentId = comment.youtubeCommentId,
                replyText = replyText
            )
            postResult.onSuccess {
                recentReplies.add(0, replyText)
                val nowIso = currentIsoUtc()
                firestoreRepo.updateCommentStatus(
                    userId = currentUserId,
                    youtubeCommentId = comment.youtubeCommentId,
                    detectedLanguage = analysis.detectedLanguage,
                    status = CommentStatus.REPLIED,
                    generatedReply = replyText,
                    failureReason = "",
                    repliedAtIso = nowIso
                )
                firestoreRepo.recordReplyHistory(
                    userId = currentUserId,
                    item = ReplyHistoryItem(
                        userId = currentUserId,
                        youtubeCommentId = comment.youtubeCommentId,
                        videoId = comment.videoId,
                        videoTitle = comment.videoTitle,
                        authorName = comment.authorName,
                        originalComment = comment.commentText,
                        aiReply = replyText,
                        languageUsed = analysis.detectedLanguage,
                        status = ReplyStatus.SENT.name,
                        errorMessage = "",
                        postedToYoutube = true
                    )
                )
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "YouTube API reply failed"
                firestoreRepo.updateCommentStatus(
                    userId = currentUserId,
                    youtubeCommentId = comment.youtubeCommentId,
                    detectedLanguage = analysis.detectedLanguage,
                    status = CommentStatus.FAILED,
                    generatedReply = replyText,
                    failureReason = msg,
                    repliedAtIso = ""
                )
                firestoreRepo.recordReplyHistory(
                    userId = currentUserId,
                    item = ReplyHistoryItem(
                        userId = currentUserId,
                        youtubeCommentId = comment.youtubeCommentId,
                        videoId = comment.videoId,
                        videoTitle = comment.videoTitle,
                        authorName = comment.authorName,
                        originalComment = comment.commentText,
                        aiReply = replyText,
                        languageUsed = analysis.detectedLanguage,
                        status = ReplyStatus.FAILED.name,
                        errorMessage = msg,
                        postedToYoutube = false
                    )
                )
            }
        }.onFailure { err ->
            val msg = err.localizedMessage ?: "Gemini AI generation failed"
            Log.e(TAG, "AI generation failed for ${comment.youtubeCommentId}", err)
            firestoreRepo.updateCommentStatus(
                userId = currentUserId,
                youtubeCommentId = comment.youtubeCommentId,
                detectedLanguage = comment.detectedLanguage,
                status = CommentStatus.FAILED,
                generatedReply = "",
                failureReason = msg,
                repliedAtIso = ""
            )
        }
    }

    fun ignoreComment(comment: CommentRecord) {
        viewModelScope.launch {
            firestoreRepo.updateCommentStatus(
                userId = currentUserId,
                youtubeCommentId = comment.youtubeCommentId,
                detectedLanguage = comment.detectedLanguage,
                status = CommentStatus.IGNORED,
                generatedReply = "",
                failureReason = "Ignored manually",
                repliedAtIso = ""
            )
            recomputeAndSyncStats()
        }
    }

    fun deleteHistoryItem(docId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteHistoryItem(currentUserId, docId)
        }
    }

    private suspend fun recomputeAndSyncStats() {
        val comments = (commentsState.value as? UiState.Success)?.data.orEmpty()
        val received = comments.size.toLong()
        val replied = comments.count { it.status == CommentStatus.REPLIED.name }.toLong()
        val pending = comments.count { it.status == CommentStatus.PENDING.name }.toLong()
        val failed = comments.count { it.status == CommentStatus.FAILED.name }.toLong()
        val skipped = comments.count {
            it.status == CommentStatus.SKIPPED_SPAM.name ||
                it.status == CommentStatus.SKIPPED_ABUSIVE.name
        }.toLong()
        firestoreRepo.updateTelemetryStats(
            userId = currentUserId,
            totalCommentsReceived = received,
            totalRepliesSent = replied,
            pendingCommentsCount = pending,
            failedRepliesCount = failed,
            skippedSpamCount = skipped
        )
    }

    fun saveLanguageSettings(
        autoDetect: Boolean,
        preferredLanguage: LanguageMode,
        enabledLanguages: List<String>
    ) {
        viewModelScope.launch {
            firestoreRepo.updateLanguageSettings(
                userId = currentUserId,
                autoDetectLanguage = autoDetect,
                preferredLanguage = preferredLanguage,
                enabledLanguages = enabledLanguages
            ).onSuccess {
                _runtime.update { it.copy(statusBanner = "Language settings saved.") }
            }.onFailure { err ->
                reportError(err.localizedMessage ?: "Failed to save language settings.")
            }
        }
    }

    fun saveCustomPrompt(prompt: String) {
        viewModelScope.launch {
            firestoreRepo.updateCustomPrompt(currentUserId, prompt)
                .onSuccess {
                    _runtime.update { it.copy(statusBanner = "Custom AI prompt saved.") }
                }
                .onFailure { err ->
                    reportError(err.localizedMessage ?: "Failed to save AI prompt.")
                }
        }
    }

    fun saveReplySettings(
        ignoreSpam: Boolean,
        ignoreAbusive: Boolean,
        avoidDuplicates: Boolean,
        replyTone: ReplyTone,
        maxReplyLength: Long,
        pollingIntervalSeconds: Long
    ) {
        viewModelScope.launch {
            firestoreRepo.updateReplySettings(
                userId = currentUserId,
                ignoreSpam = ignoreSpam,
                ignoreAbusive = ignoreAbusive,
                avoidDuplicates = avoidDuplicates,
                replyTone = replyTone,
                maxReplyLength = maxReplyLength,
                pollingIntervalSeconds = pollingIntervalSeconds
            ).onSuccess {
                _runtime.update { it.copy(statusBanner = "AI reply settings updated.") }
            }.onFailure { err ->
                reportError(err.localizedMessage ?: "Failed to update AI settings.")
            }
        }
    }

    fun testAiReplyGeneration(sampleComment: String, authorName: String = "FreeFireFan99") {
        if (sampleComment.isBlank()) return
        viewModelScope.launch {
            _runtime.update {
                it.copy(isTestingAi = true, aiTestResult = null, aiTestError = null)
            }
            val settings = (settingsState.value as? UiState.Success)?.data ?: AiSettings(userId = currentUserId)
            val result = geminiEngine.analyzeAndGenerateReply(
                commentText = sampleComment.trim(),
                authorName = authorName,
                videoTitle = "Free Fire Ranked Clash Squad Tips & Tricks",
                settings = settings
            )
            result.onSuccess { analysis ->
                _runtime.update {
                    it.copy(isTestingAi = false, aiTestResult = analysis, aiTestError = null)
                }
            }.onFailure { err ->
                _runtime.update {
                    it.copy(
                        isTestingAi = false,
                        aiTestError = err.localizedMessage ?: "AI reply preview failed."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoReplyEngineInternal()
    }

    private fun currentIsoUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun currentTimeShort(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
