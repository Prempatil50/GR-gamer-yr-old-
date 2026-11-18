package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val DEFAULT_AI_PROMPT =
    "You are GR GAMER AI, an intelligent gaming YouTube assistant. You manage comments for a Free Fire gaming YouTube channel. Reply naturally, politely, and enthusiastically. Understand every comment before replying. Use Hindi, Hinglish, or English according to the comment. Keep replies short, friendly, engaging, and relevant. Never generate spam, abusive content, fake promises, or misleading information. Avoid repeating the same reply."

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class LanguageMode(val label: String, val subtitle: String) {
    AUTO("Auto-Detect (Smart)", "Detects comment language & replies in the same language"),
    ENGLISH("English", "Reply exclusively in English"),
    HINDI("Hindi (हिन्दी)", "Reply in natural Hindi Devanagari"),
    HINGLISH("Hinglish", "Reply in casual Hindi + English gaming style");

    companion object {
        fun fromRaw(raw: String?): LanguageMode =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class ReplyTone(val label: String, val instruction: String) {
    FRIENDLY("Friendly Creator", "Warm, appreciative, welcoming to every subscriber"),
    HYPE_GAMER("Hype Gamer", "Energetic Free Fire esports vibe, encouraging and upbeat"),
    PRO_COACH("Pro Coach", "Helpful tips, tactical Free Fire advice, respectful mentor tone"),
    CALM_HELPER("Calm Helper", "Clear, concise, polite and informative");

    companion object {
        fun fromRaw(raw: String?): ReplyTone =
            entries.firstOrNull { it.name == raw } ?: HYPE_GAMER
    }
}

enum class CommentStatus(val label: String) {
    PENDING("Pending"),
    REPLIED("Replied"),
    SKIPPED_SPAM("Spam Skipped"),
    SKIPPED_ABUSIVE("Abuse Filtered"),
    FAILED("Failed"),
    IGNORED("Ignored");

    companion object {
        fun fromRaw(raw: String?): CommentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class ReplyStatus(val label: String) {
    SENT("Sent"),
    FAILED("Failed"),
    SKIPPED("Skipped");

    companion object {
        fun fromRaw(raw: String?): ReplyStatus =
            entries.firstOrNull { it.name == raw } ?: SENT
    }
}

@Keep
@IgnoreExtraProperties
data class ChannelConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val connected: Boolean = false,
    val channelId: String = "",
    val channelTitle: String = "",
    val channelHandle: String = "",
    val thumbnailUrl: String = "",
    val subscriberCount: Long = 0L,
    val videoCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val autoReplyActive: Boolean = false,
    val ignoreSpam: Boolean = true,
    val ignoreAbusive: Boolean = true,
    val avoidDuplicates: Boolean = true,
    val autoDetectLanguage: Boolean = true,
    val preferredLanguage: String = LanguageMode.AUTO.name,
    val enabledLanguages: List<String> = listOf("ENGLISH", "HINDI", "HINGLISH"),
    val replyTone: String = ReplyTone.HYPE_GAMER.name,
    val maxReplyLength: Long = 180L,
    val pollingIntervalSeconds: Long = 30L,
    val customPrompt: String = DEFAULT_AI_PROMPT,
    val totalCommentsReceived: Long = 0L,
    val totalRepliesSent: Long = 0L,
    val pendingCommentsCount: Long = 0L,
    val failedRepliesCount: Long = 0L,
    val skippedSpamCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val youtubeCommentId: String = "",
    val videoId: String = "",
    val videoTitle: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val commentText: String = "",
    val detectedLanguage: String = "UNKNOWN",
    val status: String = CommentStatus.PENDING.name,
    val generatedReply: String = "",
    val failureReason: String = "",
    val publishedAtIso: String = "",
    val repliedAtIso: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReplyHistoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val youtubeCommentId: String = "",
    val videoId: String = "",
    val videoTitle: String = "",
    val authorName: String = "",
    val originalComment: String = "",
    val aiReply: String = "",
    val languageUsed: String = "ENGLISH",
    val status: String = ReplyStatus.SENT.name,
    val errorMessage: String = "",
    val postedToYoutube: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class YouTubeChannelOption(
    val channelId: String,
    val title: String,
    val handle: String,
    val thumbnailUrl: String,
    val subscriberCount: Long,
    val videoCount: Long
)

data class AiAnalysisResult(
    val detectedLanguage: String,
    val classification: String,
    val replyText: String,
    val reason: String
)

fun sanitizeFirestoreDocId(raw: String): String {
    val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    return if (cleaned.isBlank()) "doc_${System.currentTimeMillis()}" else cleaned.take(120)
}
