package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.model.ReplyHistoryItem
import com.example.data.model.ReplyStatus
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

@Composable
fun ReplyHistoryScreen(
    history: List<ReplyHistoryItem>,
    showBackArrow: Boolean = true,
    onBack: () -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<ReplyStatus?>(null) }

    val filtered = remember(history, searchQuery, statusFilter) {
        history.filter { item ->
            val matchesFilter = statusFilter == null || item.status == statusFilter?.name
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                item.authorName.contains(q, ignoreCase = true) ||
                item.originalComment.contains(q, ignoreCase = true) ||
                item.aiReply.contains(q, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            if (showBackArrow) {
                GrGamerSubTopBar(
                    title = "Reply History",
                    onBack = onBack
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search comments, authors, or replies...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All (${history.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = CarbonCard,
                        labelColor = TextSecondary
                    )
                )
                ReplyStatus.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(status.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = CarbonCard,
                            labelColor = TextSecondary
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
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No reply history entries yet. AI replies sent or skipped will appear here.",
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
                    items(filtered, key = { it.id.ifBlank { it.youtubeCommentId } }) { item ->
                        val status = ReplyStatus.fromRaw(item.status)
                        val color = when (status) {
                            ReplyStatus.SENT -> GamingEmerald
                            ReplyStatus.FAILED -> CrimsonAlert
                            ReplyStatus.SKIPPED -> AmberGold
                        }

                        GamingCard(glowColor = color.copy(alpha = 0.65f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(status.label, color)
                                    LanguageBadge(item.languageUsed)
                                }
                                IconButton(
                                    onClick = { onDeleteEntry(item.id.ifBlank { item.youtubeCommentId }) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete history item",
                                        tint = TextMuted
                                    )
                                }
                            }

                            Text(
                                text = "${item.authorName} • ${item.videoTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Comment: ${item.originalComment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElevatedSlate,
                                border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (item.postedToYoutube) "Posted on YouTube" else "AI Log",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.aiReply,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                            if (item.errorMessage.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = item.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
