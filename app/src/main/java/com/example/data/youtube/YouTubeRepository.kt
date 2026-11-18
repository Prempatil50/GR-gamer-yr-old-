package com.example.data.youtube

import android.util.Log
import com.example.data.model.CommentRecord
import com.example.data.model.CommentStatus
import com.example.data.model.YouTubeChannelOption
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class FetchedYouTubeComment(
    val record: CommentRecord,
    val alreadyRepliedByOwnerOnYoutube: Boolean
)

class YouTubeRepository {
    private val service: YouTubeApiService by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApiService::class.java)
    }

    suspend fun fetchMyChannels(accessToken: String): Result<List<YouTubeChannelOption>> {
        return try {
            val response = service.getMyChannels("Bearer $accessToken")
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException(parseApiError(response)))
            }
            val channels = response.body()?.items.orEmpty().map { item ->
                val snippet = item.snippet
                val stats = item.statistics
                val thumb = snippet?.thumbnails?.high?.url
                    ?: snippet?.thumbnails?.medium?.url
                    ?: snippet?.thumbnails?.default?.url
                    ?: ""
                YouTubeChannelOption(
                    channelId = item.id,
                    title = snippet?.title?.ifBlank { "YouTube Channel" } ?: "YouTube Channel",
                    handle = snippet?.customUrl ?: "@youtube",
                    thumbnailUrl = thumb,
                    subscriberCount = stats?.subscriberCount?.toLongOrNull() ?: 0L,
                    videoCount = stats?.videoCount?.toLongOrNull() ?: 0L
                )
            }
            Result.success(channels)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fetchMyChannels failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchLatestComments(
        accessToken: String,
        userId: String,
        channelId: String
    ): Result<List<FetchedYouTubeComment>> {
        return try {
            val authHeader = "Bearer $accessToken"
            val response = service.getChannelCommentThreads(
                authorization = authHeader,
                channelId = channelId
            )
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException(parseApiError(response)))
            }

            val threads = response.body()?.items.orEmpty()
            val videoIds = threads.mapNotNull { it.snippet?.videoId }.distinct().take(50)
            val videoTitles = if (videoIds.isNotEmpty()) {
                runCatching {
                    val vResp = service.getVideosByIds(authHeader, ids = videoIds.joinToString(","))
                    vResp.body()?.items.orEmpty().associate { it.id to (it.snippet?.title ?: "Video") }
                }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            val fetched = threads.mapNotNull { thread ->
                val topLevel = thread.snippet?.topLevelComment ?: return@mapNotNull null
                val commentSnippet = topLevel.snippet ?: return@mapNotNull null
                val commentId = topLevel.id
                if (commentId.isBlank()) return@mapNotNull null

                // Skip comments authored by the channel owner themselves
                val authorChannelId = commentSnippet.authorChannelId?.value
                if (!authorChannelId.isNullOrBlank() && authorChannelId == channelId) {
                    return@mapNotNull null
                }

                val ownerAlreadyReplied = thread.replies?.comments.orEmpty().any { reply ->
                    reply.snippet?.authorChannelId?.value == channelId
                }

                val videoId = thread.snippet.videoId.orEmpty()
                val videoTitle = videoTitles[videoId] ?: if (videoId.isNotBlank()) "Video $videoId" else "Channel Post"
                val rawText = (commentSnippet.textOriginal ?: commentSnippet.textDisplay).orEmpty().trim()
                if (rawText.isBlank()) return@mapNotNull null

                FetchedYouTubeComment(
                    record = CommentRecord(
                        userId = userId,
                        youtubeCommentId = commentId,
                        videoId = videoId,
                        videoTitle = videoTitle,
                        authorName = commentSnippet.authorDisplayName ?: "YouTube Viewer",
                        authorAvatarUrl = commentSnippet.authorProfileImageUrl ?: "",
                        commentText = rawText,
                        detectedLanguage = "UNKNOWN",
                        status = if (ownerAlreadyReplied) CommentStatus.REPLIED.name else CommentStatus.PENDING.name,
                        generatedReply = "",
                        failureReason = "",
                        publishedAtIso = commentSnippet.publishedAt ?: "",
                        repliedAtIso = ""
                    ),
                    alreadyRepliedByOwnerOnYoutube = ownerAlreadyReplied
                )
            }
            Result.success(fetched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fetchLatestComments failed", e)
            Result.failure(e)
        }
    }

    suspend fun postReplyToComment(
        accessToken: String,
        parentCommentId: String,
        replyText: String
    ): Result<String> {
        return try {
            val response = service.postCommentReply(
                authorization = "Bearer $accessToken",
                body = YouTubeCommentReplyRequest(
                    snippet = YouTubeCommentReplySnippet(
                        parentId = parentCommentId,
                        textOriginal = replyText
                    )
                )
            )
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException(parseApiError(response)))
            }
            val postedId = response.body()?.id.orEmpty()
            Result.success(postedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "postReplyToComment failed", e)
            Result.failure(e)
        }
    }

    private fun <T> parseApiError(response: Response<T>): String {
        val raw = response.errorBody()?.string().orEmpty()
        val parsed = runCatching {
            val errObj = JSONObject(raw).optJSONObject("error")
            val message = errObj?.optString("message").orEmpty()
            val status = errObj?.optString("status").orEmpty()
            listOf(status, message).filter { it.isNotBlank() }.joinToString(": ")
        }.getOrNull()
        return if (!parsed.isNullOrBlank()) {
            "YouTube API (${response.code()}): $parsed"
        } else {
            "YouTube API request failed with HTTP ${response.code()}"
        }
    }

    companion object {
        private const val TAG = "YouTubeRepository"
    }
}
