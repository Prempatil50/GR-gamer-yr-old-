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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AiSettings
import com.example.data.model.DEFAULT_AI_PROMPT
import com.example.data.model.ReplyTone
import com.example.ui.components.GamingCard
import com.example.ui.components.GrGamerSubTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GamingEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToLong

@Composable
fun AiReplySettingsScreen(
    settings: AiSettings,
    showBackArrow: Boolean = true,
    onBack: () -> Unit,
    onSaveReplySettings: (Boolean, Boolean, Boolean, ReplyTone, Long, Long) -> Unit
) {
    var ignoreSpam by remember { mutableStateOf(settings.ignoreSpam) }
    var ignoreAbusive by remember { mutableStateOf(settings.ignoreAbusive) }
    var avoidDuplicates by remember { mutableStateOf(settings.avoidDuplicates) }
    var selectedTone by remember { mutableStateOf(ReplyTone.fromRaw(settings.replyTone)) }
    var maxLength by remember { mutableFloatStateOf(settings.maxReplyLength.toFloat()) }
    var pollInterval by remember { mutableFloatStateOf(settings.pollingIntervalSeconds.toFloat()) }

    LaunchedEffect(settings.updatedAt) {
        ignoreSpam = settings.ignoreSpam
        ignoreAbusive = settings.ignoreAbusive
        avoidDuplicates = settings.avoidDuplicates
        selectedTone = ReplyTone.fromRaw(settings.replyTone)
        maxLength = settings.maxReplyLength.toFloat()
        pollInterval = settings.pollingIntervalSeconds.toFloat()
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            if (showBackArrow) {
                GrGamerSubTopBar(
                    title = "AI Reply Settings",
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
                GamingCard(glowColor = NeonCyan) {
                    SectionHeader(
                        title = "MODERATION & SAFETY GUARDS",
                        subtitle = "Protect your Free Fire community and prevent repetitive bot replies"
                    )
                    Spacer(Modifier.height(10.dp))

                    SettingToggleRow(
                        title = "Ignore Spam Comments",
                        subtitle = "Skip promotional links, sub4sub, and scam giveaways",
                        checked = ignoreSpam,
                        onCheckedChange = { ignoreSpam = it }
                    )
                    SettingToggleRow(
                        title = "Ignore Abusive Comments",
                        subtitle = "Automatically filter toxic or harassing comments",
                        checked = ignoreAbusive,
                        onCheckedChange = { ignoreAbusive = it }
                    )
                    SettingToggleRow(
                        title = "Avoid Duplicate Replies",
                        subtitle = "Never reply twice to the same comment or repeat phrases",
                        checked = avoidDuplicates,
                        onCheckedChange = { avoidDuplicates = it }
                    )
                }
            }

            item {
                GamingCard(glowColor = ElectricBlue) {
                    SectionHeader(
                        title = "GAMING PERSONA TONE",
                        subtitle = "Select how GR GAMER AI sounds when replying"
                    )
                    Spacer(Modifier.height(8.dp))
                    ReplyTone.entries.forEach { tone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTone == tone,
                                onClick = { selectedTone = tone },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Column {
                                Text(tone.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(tone.instruction, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            item {
                GamingCard(glowColor = BorderCyan) {
                    SectionHeader(
                        title = "TIMING & LENGTH LIMITS",
                        subtitle = "Tune auto-check frequency and reply conciseness"
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Max Reply Length: ${maxLength.roundToLong()} characters",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan
                    )
                    Slider(
                        value = maxLength,
                        onValueChange = { maxLength = it },
                        valueRange = 80f..350f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = ElevatedSlate
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Auto-Check Interval: ${pollInterval.roundToLong()} seconds",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan
                    )
                    Slider(
                        value = pollInterval,
                        onValueChange = { pollInterval = it },
                        valueRange = 15f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = GamingEmerald,
                            activeTrackColor = GamingEmerald,
                            inactiveTrackColor = ElevatedSlate
                        )
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        onSaveReplySettings(
                            ignoreSpam,
                            ignoreAbusive,
                            avoidDuplicates,
                            selectedTone,
                            maxLength.roundToLong(),
                            pollInterval.roundToLong()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_ai_reply_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Text("Save AI Reply Settings", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun AiPromptSettingsScreen(
    settings: AiSettings,
    onBack: () -> Unit,
    onSavePrompt: (String) -> Unit
) {
    var promptText by remember { mutableStateOf(settings.customPrompt) }

    LaunchedEffect(settings.customPrompt) {
        promptText = settings.customPrompt
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            GrGamerSubTopBar(
                title = "AI Prompt Settings",
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
                GamingCard(glowColor = NeonCyan) {
                    SectionHeader(
                        title = "CUSTOMIZE GR GAMER AI PROMPT",
                        subtitle = "Guide how the AI understands Free Fire gaming comments and crafts natural replies."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { if (it.length <= 2000) promptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_editor"),
                        minLines = 7,
                        maxLines = 14,
                        label = { Text("System Prompt Instructions") },
                        supportingText = { Text("${promptText.length} / 2000 characters", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { promptText = DEFAULT_AI_PROMPT },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reset_default_prompt_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderCyan)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Default", color = NeonCyan)
                        }

                        Button(
                            onClick = { onSavePrompt(promptText) },
                            enabled = promptText.trim().length >= 10,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_prompt_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Text("Save Prompt", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = GamingEmerald,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ElevatedSlate
            )
        )
    }
}
