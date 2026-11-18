package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AiSettings
import com.example.data.model.ChannelConfig
import com.example.ui.components.GamingCard
import com.example.ui.components.LiveStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTelemetryCard
import com.example.ui.components.formatSubscriberCount
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CarbonCard
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

enum class DashboardDestination {
    CHANNEL_CONNECT,
    AUTO_REPLY,
    AI_SETTINGS,
    REPLY_HISTORY,
    COMMENT_MANAGEMENT,
    LANGUAGE_SETTINGS,
    AI_PROMPT_SETTINGS
}

@Composable
fun HomeDashboardScreen(
    channel: ChannelConfig,
    settings: AiSettings,
    runtime: EngineRuntimeState,
    onNavigate: (DashboardDestination) -> Unit,
    onStartAutoReply: () -> Unit,
    onStopAutoReply: () -> Unit,
    onToggleAutoReply: (Boolean) -> Unit,
    onDisconnectChannel: () -> Unit,
    onSignOut: () -> Unit
) {
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            containerColor = CarbonCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Disconnect YouTube Channel?") },
            text = {
                Text("This stops AI auto-replying and removes ${channel.channelTitle.ifBlank { "your channel" }} from GR GAMER AI. Your reply history remains safely saved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirm = false
                        onDisconnectChannel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) {
                    Text("Cancel", color = NeonCyan)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Gaming Command Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_gaming_banner),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ObsidianBlack.copy(alpha = 0.92f),
                                    ObsidianBlack.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_gr_gamer_logo),
                                contentDescription = stringResource(R.string.cd_app_logo),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, NeonCyan, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GR GAMER AI",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Gaming YouTube AI Console",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveStatusBadge(isActive = settings.autoReplyActive)
                        Text(
                            text = runtime.lastSyncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. YouTube Channel Connection Status Card
        item {
            GamingCard(
                glowColor = if (channel.connected) GamingEmerald else NeonCyan,
                onClick = { onNavigate(DashboardDestination.CHANNEL_CONNECT) }
            ) {
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
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GamingEmerald, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(ElevatedSlate),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = GamingEmerald
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.channelTitle.ifBlank { "Connected Channel" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = GamingEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${channel.handleOrFallback()} • ${formatSubscriberCount(channel.subscriberCount)} subscribers",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = if (runtime.accessToken != null) "OAuth 2.0 Authorized" else "Tap to re-authorize OAuth token",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (runtime.accessToken != null) GamingEmerald else AmberGold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YouTube Channel Not Connected",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Authorize via secure Google OAuth 2.0 to start AI replies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { onNavigate(DashboardDestination.CHANNEL_CONNECT) },
                            modifier = Modifier.testTag("dashboard_connect_channel_cta"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Text("Connect", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 3. Auto Reply Controls Deck
        item {
            GamingCard(glowColor = if (settings.autoReplyActive) GamingEmerald else ElectricBlue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.auto_reply_toggle_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                        Text(
                            text = if (settings.autoReplyActive) "Monitoring & Replying Automatically" else "Engine Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.autoReplyActive,
                        onCheckedChange = onToggleAutoReply,
                        modifier = Modifier.testTag("auto_reply_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = GamingEmerald,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ElevatedSlate
                        )
                    )
                }

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
                            .height(50.dp)
                            .testTag("start_auto_reply_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = ObsidianBlack,
                            disabledContainerColor = NeonCyan.copy(alpha = 0.25f),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "START",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onStopAutoReply,
                        enabled = settings.autoReplyActive,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("stop_auto_reply_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (settings.autoReplyActive) CrimsonAlert else BorderCyan
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CrimsonAlert,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "STOP",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 4. Live Telemetry Stats Grid
        item {
            SectionHeader(
                title = "LIVE TELEMETRY",
                subtitle = "Real-time YouTube comment & AI reply counters"
            )
            Spacer(Modifier.height(10.dp))
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

        // 5. All 8 Dashboard Modules Grid
        item {
            SectionHeader(
                title = "COMMAND MODULES",
                subtitle = "Manage your YouTube AI workflow"
            )
            Spacer(Modifier.height(10.dp))
            val tiles = listOf(
                DashboardTileSpec(
                    title = "YouTube Channel Connect",
                    subtitle = if (channel.connected) "Connected" else "OAuth 2.0",
                    icon = Icons.Default.Link,
                    accent = NeonCyan,
                    tag = "tile_channel_connect",
                    onClick = { onNavigate(DashboardDestination.CHANNEL_CONNECT) }
                ),
                DashboardTileSpec(
                    title = "AI Auto Reply",
                    subtitle = if (settings.autoReplyActive) "Active" else "Controls & Test",
                    icon = Icons.Default.AutoAwesome,
                    accent = GamingEmerald,
                    tag = "tile_ai_auto_reply",
                    onClick = { onNavigate(DashboardDestination.AUTO_REPLY) }
                ),
                DashboardTileSpec(
                    title = "AI Reply Settings",
                    subtitle = "Filters & Tone",
                    icon = Icons.Default.Tune,
                    accent = ElectricBlue,
                    tag = "tile_ai_reply_settings",
                    onClick = { onNavigate(DashboardDestination.AI_SETTINGS) }
                ),
                DashboardTileSpec(
                    title = "Reply History",
                    subtitle = "Saved Logs",
                    icon = Icons.Default.History,
                    accent = NeonCyan,
                    tag = "tile_reply_history",
                    onClick = { onNavigate(DashboardDestination.REPLY_HISTORY) }
                ),
                DashboardTileSpec(
                    title = "Comment Management",
                    subtitle = "${settings.pendingCommentsCount} Pending",
                    icon = Icons.AutoMirrored.Filled.Comment,
                    accent = AmberGold,
                    tag = "tile_comment_management",
                    onClick = { onNavigate(DashboardDestination.COMMENT_MANAGEMENT) }
                ),
                DashboardTileSpec(
                    title = "Language Settings",
                    subtitle = "Hindi • Hinglish • EN",
                    icon = Icons.Default.Translate,
                    accent = GamingEmerald,
                    tag = "tile_language_settings",
                    onClick = { onNavigate(DashboardDestination.LANGUAGE_SETTINGS) }
                ),
                DashboardTileSpec(
                    title = "AI Prompt Settings",
                    subtitle = "Custom Persona",
                    icon = Icons.Default.EditNote,
                    accent = ElectricBlue,
                    tag = "tile_ai_prompt_settings",
                    onClick = { onNavigate(DashboardDestination.AI_PROMPT_SETTINGS) }
                ),
                DashboardTileSpec(
                    title = "Disconnect YouTube Channel",
                    subtitle = if (channel.connected) "Unlink Account" else "Not Linked",
                    icon = Icons.Default.LinkOff,
                    accent = CrimsonAlert,
                    tag = "tile_disconnect_channel",
                    onClick = {
                        if (channel.connected) {
                            showDisconnectConfirm = true
                        } else {
                            onNavigate(DashboardDestination.CHANNEL_CONNECT)
                        }
                    }
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { tile ->
                            DashboardActionTile(
                                spec = tile,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardTileSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun DashboardActionTile(
    spec: DashboardTileSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = spec.onClick,
        modifier = modifier
            .height(118.dp)
            .testTag(spec.tag),
        shape = RoundedCornerShape(16.dp),
        color = CarbonCard,
        border = BorderStroke(1.dp, spec.accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(spec.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = spec.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun ChannelConfig.handleOrFallback(): String =
    channelHandle.ifBlank { "@youtube" }
