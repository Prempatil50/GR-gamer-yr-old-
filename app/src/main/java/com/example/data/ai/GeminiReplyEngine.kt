package com.example.data.ai

import android.util.Log
import com.example.data.model.AiAnalysisResult
import com.example.data.model.AiSettings
import com.example.data.model.LanguageMode
import com.example.data.model.ReplyTone
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class GeminiReplyEngine {
    private val replySchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.enumeration(
                values = listOf("ENGLISH", "HINDI", "HINGLISH"),
                description = "Detected language of the viewer comment."
            ),
            "classification" to Schema.enumeration(
                values = listOf("SAFE_REPLY", "SPAM", "ABUSIVE"),
                description = "Safety & moderation classification of the comment."
            ),
            "replyText" to Schema.string(
                description = "Natural, concise gaming reply matching the requested language and tone. Empty string if SPAM or ABUSIVE and skipped."
            ),
            "reason" to Schema.string(
                description = "Brief explanation of language detection and moderation decision."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = replySchema
                temperature = 0.7f
            }
        )
    }

    suspend fun analyzeAndGenerateReply(
        commentText: String,
        authorName: String,
        videoTitle: String,
        settings: AiSettings,
        recentReplies: List<String> = emptyList()
    ): Result<AiAnalysisResult> {
        return try {
            val tone = ReplyTone.fromRaw(settings.replyTone)
            val languageDirective = if (settings.autoDetectLanguage || settings.preferredLanguage == LanguageMode.AUTO.name) {
                val allowed = settings.enabledLanguages.joinToString(", ").ifBlank { "ENGLISH, HINDI, HINGLISH" }
                "Automatically detect whether the comment is ENGLISH, HINDI (Devanagari), or HINGLISH (Hindi written in Latin/Roman script mixed with English). Reply in the EXACT SAME language as the comment, restricted to enabled languages: [$allowed]."
            } else {
                "Reply strictly in ${settings.preferredLanguage}, while still accurately reporting detectedLanguage of the original comment."
            }

            val moderationRule = buildString {
                if (settings.ignoreSpam) {
                    append("Mark promotional links, sub4sub, scam giveaways, crypto/betting promos, or bot spam as classification='SPAM'. ")
                }
                if (settings.ignoreAbusive) {
                    append("Mark profanity, hate speech, personal insults, harassment, or toxic abuse as classification='ABUSIVE'. ")
                }
                append("Otherwise mark genuine viewer comments (including constructive feedback or questions) as classification='SAFE_REPLY'.")
            }

            val avoidDuplicateBlock = if (recentReplies.isNotEmpty() && settings.avoidDuplicates) {
                "Do NOT repeat or closely paraphrase any of these recent replies:\n" +
                    recentReplies.take(6).joinToString("\n") { "- $it" }
            } else {
                "Keep the reply unique and tailored specifically to this viewer's message."
            }

            val prompt = """
                SYSTEM INSTRUCTIONS:
                ${settings.customPrompt}

                CHANNEL & VIDEO CONTEXT:
                - Video Title: $videoTitle
                - Viewer Name: $authorName
                - Reply Tone: ${tone.label} (${tone.instruction})
                - Max Reply Length: ${settings.maxReplyLength} characters

                LANGUAGE RULE:
                $languageDirective

                MODERATION RULE:
                $moderationRule

                UNIQUENESS RULE:
                $avoidDuplicateBlock

                VIEWER COMMENT TO ANALYZE AND REPLY TO:
                "$commentText"
            """.trimIndent()

            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(rawJson)
            val detectedLang = json.optString("detectedLanguage", "ENGLISH").uppercase()
                .let { if (it in setOf("ENGLISH", "HINDI", "HINGLISH")) it else "ENGLISH" }
            val classification = json.optString("classification", "SAFE_REPLY").uppercase()
                .let { if (it in setOf("SAFE_REPLY", "SPAM", "ABUSIVE")) it else "SAFE_REPLY" }
            val replyText = json.optString("replyText", "").trim()
                .take(settings.maxReplyLength.toInt().coerceIn(50, 500))
            val reason = json.optString("reason", "").trim()

            Result.success(
                AiAnalysisResult(
                    detectedLanguage = detectedLang,
                    classification = classification,
                    replyText = replyText,
                    reason = reason
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI reply timed out", e)
            Result.failure(IllegalStateException("AI reply generation timed out after 45 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI reply failed", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "GeminiReplyEngine"
    }
}
