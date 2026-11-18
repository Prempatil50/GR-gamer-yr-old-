package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiSettings
import com.example.data.model.ChannelConfig
import com.example.ui.components.GamingCard
import com.example.ui.components.GrGamerSubTopBar
import com.example.ui.components.LanguageBadge
import com.example.ui.components.LiveStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTelemetryCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GamingEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.EngineRuntimeState

@Composable
fun AutoReplyScreen(
    channel: ChannelConfig,
    settings: AiSettings,
    runtime: EngineRuntimeState,
    showBackArrow: Boolean = true,
    onBack: () -> Unit,
    onStartAutoReply: () -> Unit,
    onStopAutoReply: () -> Unit,
    onToggleAutoReply: (Boolean) -> Unit,
    onSyncAndReplyNow: () -> Unit,
    onTestAiReply: (String) -> Unit
) {
    var testCommentInput by remember {
        mutableStateOf("Bhai Free Fire OB update ke baad best sensitivity kya rakhein?")
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            if (showBackArrow) {
                GrGamerSubTopBar(
                    title = "AI Auto Reply",
                    onBack = onBack
                )
            }
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
                GamingCard(glowColor = if (settings.autoReplyActive) GamingEmerald else NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTO REPLY ENGINE",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan
                            )
                            Spacer(Modifier.height(4.dp))
                            LiveStatusBadge(isActive = settings.autoReplyActive)
                        }
                        Switch(
                            checked = settings.autoReplyActive,
                            onCheckedChange = onToggleAutoReply,
                            modifier = Modifier.testTag("auto_reply_screen_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = GamingEmerald,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ElevatedSlate
                            )
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = if (channel.connected) {
                            "Channel: ${channel.channelTitle} • Poll interval: ${settings.pollingIntervalSeconds}s"
                        } else {
                            "Connect your YouTube channel first to enable automatic posting."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onStartAutoReply,
                            enabled = !settings.autoReplyActive,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("auto_reply_start_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianBlack,
                                disabledContainerColor = NeonCyan.copy(alpha = 0.25f),
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.start_auto_reply), style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = onStopAutoReply,
                            enabled = settings.autoReplyActive,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("auto_reply_stop_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (settings.autoReplyActive) CrimsonAlert else BorderCyan),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CrimsonAlert,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.stop_auto_reply), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onSyncAndReplyNow,
                        enabled = channel.connected && !runtime.isSyncingComments && !runtime.isProcessingReplies,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_and_reply_now_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricBlue)
                    ) {
                        if (runtime.isSyncingComments || runtime.isProcessingReplies) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing & Generating AI Replies...", color = NeonCyan)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = NeonCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Fetch & Reply Now", color = NeonCyan)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTelemetryCard(
                            title = "Total Comments Received",
                            value = settings.totalCommentsReceived,
                            accentColor = NeonCyan,
                            icon = Icons.AutoMirrored.Filled.Comment,
                            modifier = Modifier.weight(1f)
                        )
                        StatTelemetryCard(
                            title = "Total Replies Sent",
                            value = settings.totalRepliesSent,
                            accentColor = GamingEmerald,
                            icon = Icons.Default.MarkChatRead,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTelemetryCard(
                            title = "Pending Comments",
                            value = settings.pendingCommentsCount,
                            accentColor = AmberGold,
                            icon = Icons.Default.HourglassTop,
                            modifier = Modifier.weight(1f)
                        )
                        StatTelemetryCard(
                            title = "Failed Replies",
                            value = settings.failedRepliesCount,
                            accentColor = CrimsonAlert,
                            icon = Icons.Default.ErrorOutline,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Live Gemini AI Reply Sandbox Tester (uses real Firebase.ai Gemini model!)
            item {
                GamingCard(glowColor = ElectricBlue) {
                    SectionHeader(
                        title = "AI REPLY SANDBOX (LIVE GEMINI TEST)",
                        subtitle = "Test how GR GAMER AI responds to English, Hindi, or Hinglish comments using your active prompt & filters."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testCommentInput,
                        onValueChange = { testCommentInput = it },
                        label = { Text("Sample Viewer Comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_sandbox_input"),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "English" to "Awesome clutch bro! Which character skill combo do you use?",
                            "Hindi" to "भाई बहुत बढ़िया गेमप्ले था, अगला लाइव कब करोगे?",
                            "Hinglish" to "Bhai headshot sensitivity batao please OP gameplay!"
                        ).forEach { (label, preset) ->
                            OutlinedButton(
                                onClick = { testCommentInput = preset },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, BorderCyan)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onTestAiReply(testCommentInput) },
                        enabled = !runtime.isTestingAi && testCommentInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_ai_reply_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = TextPrimary
                        )
                    ) {
                        if (runtime.isTestingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generating with Gemini AI...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate AI Reply Preview", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    runtime.aiTestError?.let { err ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonAlert
                        )
                    }

                    runtime.aiTestResult?.let { res ->
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ElevatedSlate,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LanguageBadge(res.detectedLanguage)
                                    val classColor = when (res.classification) {
                                        "SAFE_REPLY" -> GamingEmerald
                                        "SPAM" -> AmberGold
                                        else -> CrimsonAlert
                                    }
                                    StatusPill(res.classification, classColor)
                                }
                                Text(
                                    text = if (res.replyText.isNotBlank()) res.replyText else "Skipped (${res.reason})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                if (res.reason.isNotBlank()) {
                                    Text(
                                        text = "AI Note: ${res.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
