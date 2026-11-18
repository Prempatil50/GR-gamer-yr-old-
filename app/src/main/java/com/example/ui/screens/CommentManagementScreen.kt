package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ChannelConfig
import com.example.data.model.CommentRecord
import com.example.data.model.CommentStatus
import com.example.ui.components.GamingCard
import com.example.ui.components.GrGamerSubTopBar
import com.example.ui.components.LanguageBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GamingEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.EngineRuntimeState

private enum class CommentFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    REPLIED("Replied"),
    SKIPPED("Skipped"),
    FAILED("Failed")
}

@Composable
fun CommentManagementScreen(
    channel: ChannelConfig,
    comments: List<CommentRecord>,
    runtime: EngineRuntimeState,
    showBackArrow: Boolean = true,
    onBack: () -> Unit,
    onFetchComments: () -> Unit,
    onReplyToComment: (CommentRecord) -> Unit,
    onIgnoreComment: (CommentRecord) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(CommentFilter.ALL) }

    val filtered = remember(comments, selectedFilter) {
        when (selectedFilter) {
            CommentFilter.ALL -> comments
            CommentFilter.PENDING -> comments.filter { it.status == CommentStatus.PENDING.name }
            CommentFilter.REPLIED -> comments.filter { it.status == CommentStatus.REPLIED.name }
            CommentFilter.SKIPPED -> comments.filter {
                it.status == CommentStatus.SKIPPED_SPAM.name ||
                    it.status == CommentStatus.SKIPPED_ABUSIVE.name ||
                    it.status == CommentStatus.IGNORED.name
            }
            CommentFilter.FAILED -> comments.filter { it.status == CommentStatus.FAILED.name }
        }
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            if (showBackArrow) {
                GrGamerSubTopBar(
                    title = "Comment Management",
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = onFetchComments,
                            enabled = channel.connected && !runtime.isSyncingComments,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_comments_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Fetch comments",
                                tint = NeonCyan
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${comments.size} Total Comments",
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonCyan
                )
                Button(
                    onClick = onFetchComments,
                    enabled = channel.connected && !runtime.isSyncingComments,
                    modifier = Modifier.testTag("fetch_live_comments_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = ObsidianBlack
                    )
                ) {
                    if (runtime.isSyncingComments) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ObsidianBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Syncing...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sync YouTube", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommentFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = CarbonCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = BorderCyan,
                            selectedBorderColor = NeonCyan
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (channel.connected) {
                                "No comments in ${selectedFilter.label}. Tap Sync YouTube to fetch latest comments."
                            } else {
                                "Connect your YouTube channel first to sync live comments."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.youtubeCommentId.ifBlank { it.id } }) { comment ->
                        CommentItemCard(
                            comment = comment,
                            isProcessing = runtime.processingCommentId == comment.youtubeCommentId,
                            onReplyNow = { onReplyToComment(comment) },
                            onIgnore = { onIgnoreComment(comment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItemCard(
    comment: CommentRecord,
    isProcessing: Boolean,
    onReplyNow: () -> Unit,
    onIgnore: () -> Unit
) {
    val status = CommentStatus.fromRaw(comment.status)
    val statusColor = when (status) {
        CommentStatus.REPLIED -> GamingEmerald
        CommentStatus.PENDING -> AmberGold
        CommentStatus.FAILED -> CrimsonAlert
        CommentStatus.SKIPPED_SPAM, CommentStatus.SKIPPED_ABUSIVE, CommentStatus.IGNORED -> TextMuted
    }

    GamingCard(glowColor = statusColor.copy(alpha = 0.7f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (comment.authorAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = comment.authorAvatarUrl,
                    contentDescription = comment.authorName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.authorName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = comment.videoTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(status.label, statusColor)
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = comment.commentText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (comment.detectedLanguage != "UNKNOWN") {
                LanguageBadge(comment.detectedLanguage)
            }
        }

        if (comment.generatedReply.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElevatedSlate,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "GR GAMER AI Reply",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = comment.generatedReply,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        if (comment.failureReason.isNotBlank() && status == CommentStatus.FAILED) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = comment.failureReason,
                style = MaterialTheme.typography.bodySmall,
                color = CrimsonAlert
            )
        }

        if (status == CommentStatus.PENDING || status == CommentStatus.FAILED) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReplyNow,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = ObsidianBlack
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ObsidianBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Replying...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (status == CommentStatus.FAILED) "Retry AI Reply" else "AI Reply & Post")
                    }
                }

                OutlinedButton(
                    onClick = onIgnore,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderCyan)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ignore", color = TextSecondary)
                }
            }
        }
    }
}
