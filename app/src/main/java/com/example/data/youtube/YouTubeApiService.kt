package com.example.data.youtube

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("youtube/v3/channels")
    suspend fun getMyChannels(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet,statistics",
        @Query("mine") mine: Boolean = true
    ): Response<YouTubeChannelListResponse>

    @GET("youtube/v3/commentThreads")
    suspend fun getChannelCommentThreads(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet,replies",
        @Query("allThreadsRelatedToChannelId") channelId: String,
        @Query("order") order: String = "time",
        @Query("maxResults") maxResults: Int = 30,
        @Query("textFormat") textFormat: String = "plainText"
    ): Response<YouTubeCommentThreadListResponse>

    @GET("youtube/v3/videos")
    suspend fun getVideosByIds(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet",
        @Query("id") ids: String
    ): Response<YouTubeVideoListResponse>

    @POST("youtube/v3/comments")
    suspend fun postCommentReply(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet",
        @Body body: YouTubeCommentReplyRequest
    ): Response<YouTubeCommentDto>
}

@JsonClass(generateAdapter = true)
data class YouTubeChannelListResponse(
    val items: List<YouTubeChannelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
    val id: String = "",
    val snippet: YouTubeChannelSnippet? = null,
    val statistics: YouTubeChannelStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelSnippet(
    val title: String = "",
    val customUrl: String? = null,
    val thumbnails: YouTubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val default: YouTubeThumbnailItem? = null,
    val medium: YouTubeThumbnailItem? = null,
    val high: YouTubeThumbnailItem? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailItem(
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelStatistics(
    val subscriberCount: String? = null,
    val videoCount: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentThreadListResponse(
    val items: List<YouTubeCommentThreadItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentThreadItem(
    val id: String = "",
    val snippet: YouTubeCommentThreadSnippet? = null,
    val replies: YouTubeCommentRepliesContainer? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentThreadSnippet(
    val videoId: String? = null,
    val topLevelComment: YouTubeCommentDto? = null,
    val totalReplyCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentRepliesContainer(
    val comments: List<YouTubeCommentDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentDto(
    val id: String = "",
    val snippet: YouTubeCommentSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentSnippet(
    val authorDisplayName: String? = null,
    val authorProfileImageUrl: String? = null,
    val authorChannelId: YouTubeAuthorChannelId? = null,
    val textDisplay: String? = null,
    val textOriginal: String? = null,
    val publishedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeAuthorChannelId(
    val value: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    val id: String = "",
    val snippet: YouTubeVideoSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoSnippet(
    val title: String = ""
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentReplyRequest(
    val snippet: YouTubeCommentReplySnippet
)

@JsonClass(generateAdapter = true)
data class YouTubeCommentReplySnippet(
    val parentId: String,
    val textOriginal: String
)
