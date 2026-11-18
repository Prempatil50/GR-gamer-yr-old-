package com.example.data.youtube

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

sealed interface YouTubeAuthOutcome {
    data class Authorized(val accessToken: String) : YouTubeAuthOutcome
    data class NeedsConsent(val intentSenderRequest: IntentSenderRequest) : YouTubeAuthOutcome
    data class Failed(
        val message: String,
        val isConfigurationError: Boolean = false
    ) : YouTubeAuthOutcome
}

object YouTubeOAuthManager {
    private const val TAG = "YouTubeOAuthManager"
    private const val YOUTUBE_FORCE_SSL_SCOPE = "https://www.googleapis.com/auth/youtube.force-ssl"

    private val YOUTUBE_SCOPES = listOf(
        Scope(YOUTUBE_FORCE_SSL_SCOPE)
    )

    suspend fun requestYouTubeAuthorization(
        activity: Activity,
        accountEmail: String? = null
    ): YouTubeAuthOutcome {
        val gmsStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        if (gmsStatus != ConnectionResult.SUCCESS) {
            return YouTubeAuthOutcome.Failed(
                message = "Google Play Services is required for OAuth 2.0 (status code $gmsStatus). Please update Google Play Services on this device.",
                isConfigurationError = true
            )
        }

        return try {
            val builder = AuthorizationRequest.builder()
                .setRequestedScopes(YOUTUBE_SCOPES)

            if (!accountEmail.isNullOrBlank()) {
                builder.setAccount(Account(accountEmail, "com.google"))
            }

            val result = Identity.getAuthorizationClient(activity)
                .authorize(builder.build())
                .await()

            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent != null) {
                    YouTubeAuthOutcome.NeedsConsent(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    )
                } else {
                    YouTubeAuthOutcome.Failed(
                        message = "Google OAuth consent intent was unavailable.",
                        isConfigurationError = true
                    )
                }
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    YouTubeAuthOutcome.Authorized(token)
                } else {
                    YouTubeAuthOutcome.Failed(
                        message = "Google OAuth completed without returning an access token."
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Log.e(TAG, "YouTube OAuth ApiException code=${e.statusCode}", e)
            val mapped = mapApiException(e.statusCode, e.localizedMessage)
            YouTubeAuthOutcome.Failed(
                message = mapped.first,
                isConfigurationError = mapped.second
            )
        } catch (e: Exception) {
            Log.e(TAG, "YouTube OAuth failed", e)
            YouTubeAuthOutcome.Failed(
                message = e.localizedMessage ?: "Failed to authorize YouTube Data API v3 access."
            )
        }
    }

    fun extractTokenFromConsentResult(context: Context, data: Intent?): Result<String> {
        return try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(
                    IllegalStateException("Google OAuth consent completed without an access token.")
                )
            }
        } catch (e: ApiException) {
            Log.e(TAG, "extractToken ApiException code=${e.statusCode}", e)
            val mapped = mapApiException(e.statusCode, e.localizedMessage)
            Result.failure(IllegalStateException(mapped.first, e))
        } catch (e: Exception) {
            Log.e(TAG, "extractToken failed", e)
            Result.failure(e)
        }
    }

    private fun mapApiException(statusCode: Int, rawMessage: String?): Pair<String, Boolean> {
        val codeName = CommonStatusCodes.getStatusCodeString(statusCode)
        return when (statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR -> (
                "OAuth configuration error ($codeName). Ensure YouTube Data API v3 and the YouTube scope are enabled in Google Cloud Console for this project, and add your Google account as an OAuth Test User."
            ) to true

            CommonStatusCodes.CANCELED, 12501 -> (
                "Google OAuth consent was cancelled or blocked ($codeName). If Google blocked an unverified YouTube scope, add your email as a Test User in Google Cloud OAuth Consent Screen."
            ) to true

            CommonStatusCodes.NETWORK_ERROR -> (
                "Network error during Google OAuth ($codeName). Check internet connectivity and retry."
            ) to false

            CommonStatusCodes.SIGN_IN_REQUIRED -> (
                "No active Google account found ($codeName). Tap 'Use Different Google Account' or sign in to a Google account on this device."
            ) to false

            else -> (
                "Google OAuth failed ($codeName / $statusCode): ${rawMessage ?: "Unknown error"}"
            ) to (statusCode == CommonStatusCodes.API_NOT_CONNECTED)
        }
    }
}
