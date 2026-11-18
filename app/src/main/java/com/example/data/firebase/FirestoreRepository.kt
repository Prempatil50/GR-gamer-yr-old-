package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AiSettings
import com.example.data.model.ChannelConfig
import com.example.data.model.CommentRecord
import com.example.data.model.CommentStatus
import com.example.data.model.DEFAULT_AI_PROMPT
import com.example.data.model.LanguageMode
import com.example.data.model.ReplyHistoryItem
import com.example.data.model.ReplyTone
import com.example.data.model.YouTubeChannelOption
import com.example.data.model.sanitizeFirestoreDocId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireActiveUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    private fun channelDocRef(userId: String) =
        db.collection("users").document(userId).collection("channel_config").document(ACTIVE_DOC_ID)

    private fun aiSettingsDocRef(userId: String) =
        db.collection("users").document(userId).collection("ai_settings").document(ACTIVE_DOC_ID)

    private fun commentsCollection(userId: String) =
        db.collection("users").document(userId).collection("comments")

    private fun historyCollection(userId: String) =
        db.collection("users").document(userId).collection("reply_history")

    fun observeChannelConfig(userId: String): Flow<ChannelConfig> =
        channelDocRef(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    ChannelConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: ChannelConfig(userId = userId, connected = false)
            }

    fun observeAiSettings(userId: String): Flow<AiSettings> =
        aiSettingsDocRef(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    AiSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: AiSettings(userId = userId)
            }

    fun observeComments(userId: String): Flow<List<CommentRecord>> =
        commentsCollection(userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CommentRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeReplyHistory(userId: String): Flow<List<ReplyHistoryItem>> =
        historyCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ReplyHistoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun ensureDefaultAiSettings(userId: String): Result<Unit> {
        val path = "users/$userId/ai_settings/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            val docRef = aiSettingsDocRef(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "autoReplyActive" to false,
                    "ignoreSpam" to true,
                    "ignoreAbusive" to true,
                    "avoidDuplicates" to true,
                    "autoDetectLanguage" to true,
                    "preferredLanguage" to LanguageMode.AUTO.name,
                    "enabledLanguages" to listOf("ENGLISH", "HINDI", "HINGLISH"),
                    "replyTone" to ReplyTone.HYPE_GAMER.name,
                    "maxReplyLength" to 180L,
                    "pollingIntervalSeconds" to 30L,
                    "customPrompt" to DEFAULT_AI_PROMPT,
                    "totalCommentsReceived" to 0L,
                    "totalRepliesSent" to 0L,
                    "pendingCommentsCount" to 0L,
                    "failedRepliesCount" to 0L,
                    "skippedSpamCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveConnectedChannel(
        userId: String,
        channel: YouTubeChannelOption
    ): Result<Unit> {
        val path = "users/$userId/channel_config/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            val docRef = channelDocRef(uid)
            val exists = docRef.get().await().exists()
            val safeChannelId = channel.channelId.take(128)
            val safeTitle = channel.title.take(120)
            val safeHandle = channel.handle.take(100)
            val safeThumb = channel.thumbnailUrl.take(500)
            val safeSubs = channel.subscriberCount.coerceAtLeast(0L)
            val safeVideos = channel.videoCount.coerceAtLeast(0L)

            if (exists) {
                docRef.update(
                    mapOf(
                        "connected" to true,
                        "channelId" to safeChannelId,
                        "channelTitle" to safeTitle,
                        "channelHandle" to safeHandle,
                        "thumbnailUrl" to safeThumb,
                        "subscriberCount" to safeSubs,
                        "videoCount" to safeVideos,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "connected" to true,
                        "channelId" to safeChannelId,
                        "channelTitle" to safeTitle,
                        "channelHandle" to safeHandle,
                        "thumbnailUrl" to safeThumb,
                        "subscriberCount" to safeSubs,
                        "videoCount" to safeVideos,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun disconnectChannel(userId: String): Result<Unit> {
        val path = "users/$userId/channel_config/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            val docRef = channelDocRef(uid)
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "connected" to false,
                        "channelId" to "",
                        "channelTitle" to "",
                        "channelHandle" to "",
                        "thumbnailUrl" to "",
                        "subscriberCount" to 0L,
                        "videoCount" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            // Also turn off auto-reply when channel is disconnected
            setAutoReplyActive(uid, false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setAutoReplyActive(userId: String, active: Boolean): Result<Unit> {
        val path = "users/$userId/ai_settings/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            ensureDefaultAiSettings(uid).getOrThrow()
            aiSettingsDocRef(uid).update(
                mapOf(
                    "autoReplyActive" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateLanguageSettings(
        userId: String,
        autoDetectLanguage: Boolean,
        preferredLanguage: LanguageMode,
        enabledLanguages: List<String>
    ): Result<Unit> {
        val path = "users/$userId/ai_settings/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            ensureDefaultAiSettings(uid).getOrThrow()
            val validLanguages = enabledLanguages
                .filter { it in ALLOWED_LANGUAGES }
                .distinct()
                .ifEmpty { listOf("ENGLISH") }
            aiSettingsDocRef(uid).update(
                mapOf(
                    "autoDetectLanguage" to autoDetectLanguage,
                    "preferredLanguage" to preferredLanguage.name,
                    "enabledLanguages" to validLanguages,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomPrompt(userId: String, prompt: String): Result<Unit> {
        val path = "users/$userId/ai_settings/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            val trimmed = prompt.trim()
            require(trimmed.length >= 10) { "AI prompt must be at least 10 characters." }
            require(trimmed.length <= 2000) { "AI prompt must be 2000 characters or fewer." }
            ensureDefaultAiSettings(uid).getOrThrow()
            aiSettingsDocRef(uid).update(
                mapOf(
                    "customPrompt" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateReplySettings(
        userId: String,
        ignoreSpam: Boolean,
        ignoreAbusive: Boolean,
        avoidDuplicates: Boolean,
        replyTone: ReplyTone,
        maxReplyLength: Long,
        pollingIntervalSeconds: Long
    ): Result<Unit> {
        val path = "users/$userId/ai_settings/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            ensureDefaultAiSettings(uid).getOrThrow()
            aiSettingsDocRef(uid).update(
                mapOf(
                    "ignoreSpam" to ignoreSpam,
                    "ignoreAbusive" to ignoreAbusive,
                    "avoidDuplicates" to avoidDuplicates,
                    "replyTone" to replyTone.name,
                    "maxReplyLength" to maxReplyLength.coerceIn(50L, 500L),
                    "pollingIntervalSeconds" to pollingIntervalSeconds.coerceIn(15L, 300L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTelemetryStats(
        userId: String,
        totalCommentsReceived: Long,
        totalRepliesSent: Long,
        pendingCommentsCount: Long,
        failedRepliesCount: Long,
        skippedSpamCount: Long
    ): Result<Unit> {
        val path = "users/$userId/ai_settings/$ACTIVE_DOC_ID"
        return try {
            val uid = requireActiveUid(userId)
            ensureDefaultAiSettings(uid).getOrThrow()
            aiSettingsDocRef(uid).update(
                mapOf(
                    "totalCommentsReceived" to totalCommentsReceived.coerceAtLeast(0L),
                    "totalRepliesSent" to totalRepliesSent.coerceAtLeast(0L),
                    "pendingCommentsCount" to pendingCommentsCount.coerceAtLeast(0L),
                    "failedRepliesCount" to failedRepliesCount.coerceAtLeast(0L),
                    "skippedSpamCount" to skippedSpamCount.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertFetchedComment(
        userId: String,
        record: CommentRecord
    ): Result<Boolean> {
        val docId = sanitizeFirestoreDocId(record.youtubeCommentId)
        val path = "users/$userId/comments/$docId"
        return try {
            val uid = requireActiveUid(userId)
            val docRef = commentsCollection(uid).document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeText = record.commentText.trim().take(3000).ifBlank { "(Empty comment)" }
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "youtubeCommentId" to record.youtubeCommentId.take(200),
                        "videoId" to record.videoId.take(100),
                        "videoTitle" to record.videoTitle.take(250),
                        "authorName" to record.authorName.take(150),
                        "authorAvatarUrl" to record.authorAvatarUrl.take(500),
                        "commentText" to safeText,
                        "detectedLanguage" to record.detectedLanguage.take(20),
                        "status" to record.status,
                        "generatedReply" to record.generatedReply.take(1000),
                        "failureReason" to record.failureReason.take(500),
                        "publishedAtIso" to record.publishedAtIso.take(64),
                        "repliedAtIso" to record.repliedAtIso.take(64),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCommentStatus(
        userId: String,
        youtubeCommentId: String,
        detectedLanguage: String,
        status: CommentStatus,
        generatedReply: String,
        failureReason: String,
        repliedAtIso: String
    ): Result<Unit> {
        val docId = sanitizeFirestoreDocId(youtubeCommentId)
        val path = "users/$userId/comments/$docId"
        return try {
            val uid = requireActiveUid(userId)
            val docRef = commentsCollection(uid).document(docId)
            val safeLang = if (detectedLanguage in listOf("ENGLISH", "HINDI", "HINGLISH", "UNKNOWN")) {
                detectedLanguage
            } else {
                "UNKNOWN"
            }
            docRef.update(
                mapOf(
                    "detectedLanguage" to safeLang,
                    "status" to status.name,
                    "generatedReply" to generatedReply.take(1000),
                    "failureReason" to failureReason.take(500),
                    "repliedAtIso" to repliedAtIso.take(64),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordReplyHistory(
        userId: String,
        item: ReplyHistoryItem
    ): Result<Unit> {
        val docId = sanitizeFirestoreDocId(item.youtubeCommentId)
        val path = "users/$userId/reply_history/$docId"
        return try {
            val uid = requireActiveUid(userId)
            val docRef = historyCollection(uid).document(docId)
            val safeLang = if (item.languageUsed in ALLOWED_LANGUAGES) item.languageUsed else "ENGLISH"
            val safeReply = item.aiReply.trim().take(1000).ifBlank { "(Skipped)" }
            val safeOriginal = item.originalComment.trim().take(3000).ifBlank { "(Comment)" }
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "aiReply" to safeReply,
                        "languageUsed" to safeLang,
                        "status" to item.status,
                        "errorMessage" to item.errorMessage.take(500),
                        "postedToYoutube" to item.postedToYoutube,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "youtubeCommentId" to item.youtubeCommentId.take(200),
                        "videoId" to item.videoId.take(100),
                        "videoTitle" to item.videoTitle.take(250),
                        "authorName" to item.authorName.take(150),
                        "originalComment" to safeOriginal,
                        "aiReply" to safeReply,
                        "languageUsed" to safeLang,
                        "status" to item.status,
                        "errorMessage" to item.errorMessage.take(500),
                        "postedToYoutube" to item.postedToYoutube,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun hasAlreadyReplied(userId: String, youtubeCommentId: String): Boolean {
        val docId = sanitizeFirestoreDocId(youtubeCommentId)
        val path = "users/$userId/reply_history/$docId"
        return try {
            val uid = requireActiveUid(userId)
            val doc = historyCollection(uid).document(docId).get().await()
            doc.exists() && doc.getString("status") == "SENT"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun deleteHistoryItem(userId: String, docId: String): Result<Unit> {
        val safeId = sanitizeFirestoreDocId(docId)
        val path = "users/$userId/reply_history/$safeId"
        return try {
            val uid = requireActiveUid(userId)
            historyCollection(uid).document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        private const val ACTIVE_DOC_ID = "active"
        private val ALLOWED_LANGUAGES = setOf("ENGLISH", "HINDI", "HINGLISH")
    }
}
