package com.example.ui.screens

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ChannelConfig
import com.example.data.model.YouTubeChannelOption
import com.example.data.youtube.YouTubeAuthOutcome
import com.example.data.youtube.YouTubeOAuthManager
import com.example.ui.components.GamingCard
import com.example.ui.components.GrGamerSubTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatSubscriberCount
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GamingEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.EngineRuntimeState
import kotlinx.coroutines.launch

@Composable
fun ChannelConnectScreen(
    channel: ChannelConfig,
    userEmail: String?,
    runtime: EngineRuntimeState,
    onBack: () -> Unit,
    onStartConnecting: () -> Unit,
    onTokenAcquired: (String) -> Unit,
    onAuthError: (String, Boolean) -> Unit,
    onSelectChannel: (YouTubeChannelOption) -> Unit,
    onSwitchChannel: () -> Unit,
    onDismissChannelPicker: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDisconnectDialog by remember { mutableStateOf(false) }
    val cloudProjectId = remember(context) {
        runCatching { context.getString(R.string.project_id) }
            .getOrDefault("gen-lang-client-0917219847")
    }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            YouTubeOAuthManager.extractTokenFromConsentResult(context, activityResult.data)
                .onSuccess { token -> onTokenAcquired(token) }
                .onFailure { err ->
                    onAuthError(
                        err.localizedMessage ?: "Failed to obtain YouTube OAuth token.",
                        true
                    )
                }
        } else {
            onAuthError(
                "Google OAuth 2.0 consent was cancelled or blocked. If Google blocked access, ensure your email is added as a Test User on the OAuth Consent Screen and YouTube Data API v3 is enabled.",
                true
            )
        }
    }

    fun launchOAuthForEmail(targetEmail: String?) {
        val activity = context as? Activity
        if (activity == null) {
            onAuthError("Activity context unavailable for OAuth.", false)
            return
        }
        onStartConnecting()
        scope.launch {
            when (val outcome = YouTubeOAuthManager.requestYouTubeAuthorization(activity, targetEmail)) {
                is YouTubeAuthOutcome.Authorized -> onTokenAcquired(outcome.accessToken)
                is YouTubeAuthOutcome.NeedsConsent -> consentLauncher.launch(outcome.intentSenderRequest)
                is YouTubeAuthOutcome.Failed -> onAuthError(outcome.message, outcome.isConfigurationError)
            }
        }
    }

    // Google Account Chooser + "Add Account (Email & Password)" built-in Google flow
    val googleAccountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val chosenEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            launchOAuthForEmail(chosenEmail)
        } else {
            onAuthError("Google account selection was cancelled.", false)
        }
    }

    fun launchGoogleAccountChooser() {
        val preselected = userEmail?.takeIf { it.isNotBlank() }?.let { Account(it, "com.google") }
        val intent = AccountManager.newChooseAccountIntent(
            preselected,
            null,
            arrayOf("com.google"),
            "Choose a Google account or tap Add Account to log in with Email & Password",
            null,
            null,
            null
        )
        runCatching {
            googleAccountChooserLauncher.launch(intent)
        }.onFailure { err ->
            onAuthError(
                err.localizedMessage ?: "Unable to open Google account chooser.",
                false
            )
        }
    }

    // Direct Google Email & Password Sign-In flow via system Add Google Account screen
    val addGoogleAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val addedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!addedEmail.isNullOrBlank()) {
                launchOAuthForEmail(addedEmail)
            } else {
                launchGoogleAccountChooser()
            }
        } else {
            // Even if user backed out after adding or cancelling, offer chooser if needed
            onAuthError(
                "Google Email & Password login was not completed. Tap 'Connect with Google' to select an account or try again.",
                false
            )
        }
    }

    fun launchAddGoogleAccountEmailPassword() {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        }
        runCatching {
            addGoogleAccountLauncher.launch(intent)
        }.onFailure {
            // Fallback to chooser which also includes Add Account
            launchGoogleAccountChooser()
        }
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    if (runtime.showChannelPicker) {
        AlertDialog(
            onDismissRequest = onDismissChannelPicker,
            containerColor = CarbonCard,
            titleContentColor = NeonCyan,
            title = { Text("Select Your YouTube Channel") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(runtime.discoveredChannels, key = { it.channelId }) { option ->
                        Surface(
                            onClick = { onSelectChannel(option) },
                            shape = RoundedCornerShape(14.dp),
                            color = ElevatedSlate,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = option.thumbnailUrl,
                                    contentDescription = option.title,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${option.handle} • ${formatSubscriberCount(option.subscriberCount)} subs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissChannelPicker) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            containerColor = CarbonCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Disconnect YouTube Channel?") },
            text = { Text("Auto-reply will pause immediately and your OAuth session will be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel", color = NeonCyan)
                }
            }
        )
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            GrGamerSubTopBar(
                title = "YouTube Channel Connect",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GamingCard(glowColor = if (channel.connected) GamingEmerald else NeonCyan) {
                    if (channel.connected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (channel.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = channel.thumbnailUrl,
                                    contentDescription = stringResource(R.string.cd_channel_avatar),
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, GamingEmerald, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(ElevatedSlate),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = GamingEmerald,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = channel.channelTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Connection",
                                        tint = GamingEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = channel.channelHandle.ifBlank { channel.channelId },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "${formatSubscriberCount(channel.subscriberCount)} Subscribers",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GamingEmerald
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VideoLibrary,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${channel.videoCount} Videos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (runtime.accessToken != null) onSwitchChannel()
                                    else launchGoogleAccountChooser()
                                },
                                enabled = !runtime.isConnectingChannel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("reauthorize_channel_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (runtime.accessToken == null) "Authorize Token" else "Switch Channel",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = { showDisconnectDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("disconnect_channel_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CrimsonAlert),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Disconnect", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        SectionHeader(
                            title = "Connect Your YouTube Channel",
                            subtitle = "Choose your Google account or sign in with Email & Password via Google's official OAuth 2.0 screen."
                        )
                        Spacer(Modifier.height(14.dp))

                        // Option 1: Google Account Picker + Add Account
                        Button(
                            onClick = { launchGoogleAccountChooser() },
                            enabled = !runtime.isConnectingChannel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("connect_youtube_channel_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            if (runtime.isConnectingChannel) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ObsidianBlack,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Connecting YouTube...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Connect with Google",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Option 2: Direct Google Email & Password login
                        Button(
                            onClick = { launchAddGoogleAccountEmailPassword() },
                            enabled = !runtime.isConnectingChannel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_email_password_login_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Google Login (Email & Password)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (!userEmail.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { launchOAuthForEmail(userEmail) },
                                enabled = !runtime.isConnectingChannel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("quick_authorize_signed_in_account"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderCyan)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Quick Connect ($userEmail)", color = NeonCyan)
                            }
                        }
                    }
                }
            }

            if (!runtime.channelError.isNullOrBlank()) {
                item {
                    GamingCard(glowColor = CrimsonAlert) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CrimsonAlert
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Connection Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = CrimsonAlert
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = runtime.channelError,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )

                        if (runtime.channelNeedsCloudSetup) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Quick Fix Checklist:\n" +
                                    "1. Enable YouTube Data API v3 in Google Cloud Console.\n" +
                                    "2. Add ${userEmail ?: "your Google email"} as a Test User in OAuth Consent Screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        openExternalUrl(
                                            "https://console.cloud.google.com/apis/library/youtube.googleapis.com?project=$cloudProjectId"
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NeonCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Enable API", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                                OutlinedButton(
                                    onClick = {
                                        openExternalUrl(
                                            "https://console.cloud.google.com/apis/credentials/consent?project=$cloudProjectId"
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NeonCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("OAuth Consent", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }

            item {
                GamingCard(glowColor = BorderCyan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Google OAuth 2.0 & Cloud Project",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Cloud Project ID: $cloudProjectId\n" +
                            "• Android Package: ${context.packageName}\n" +
                            "• Tap 'Connect with Google' or 'Google Login (Email & Password)' to sign in through Google's official security screen.\n" +
                            "• GR GAMER AI never reads or stores your Google password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openExternalUrl(
                                    "https://console.cloud.google.com/apis/library/youtube.googleapis.com?project=$cloudProjectId"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_youtube_api_console_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderCyan)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("YouTube API v3", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        }
                        OutlinedButton(
                            onClick = {
                                openExternalUrl(
                                    "https://console.cloud.google.com/apis/credentials/consent?project=$cloudProjectId"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_oauth_consent_console_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderCyan)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("OAuth Consent", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}
