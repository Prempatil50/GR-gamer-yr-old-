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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AiSettings
import com.example.data.model.LanguageMode
import com.example.ui.components.GamingCard
import com.example.ui.components.GrGamerSubTopBar
import com.example.ui.components.LanguageBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GamingEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LanguageSettingsScreen(
    settings: AiSettings,
    onBack: () -> Unit,
    onSaveLanguageSettings: (Boolean, LanguageMode, List<String>) -> Unit
) {
    var autoDetect by remember { mutableStateOf(settings.autoDetectLanguage) }
    var preferredMode by remember { mutableStateOf(LanguageMode.fromRaw(settings.preferredLanguage)) }
    var enabledLanguages by remember { mutableStateOf(settings.enabledLanguages.toSet()) }

    LaunchedEffect(settings.updatedAt) {
        autoDetect = settings.autoDetectLanguage
        preferredMode = LanguageMode.fromRaw(settings.preferredLanguage)
        enabledLanguages = settings.enabledLanguages.toSet()
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            GrGamerSubTopBar(
                title = "Language Settings",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Detect Comment Language",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Automatically detect Hindi, Hinglish, or English and reply in the exact same language.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = autoDetect,
                            onCheckedChange = { checked ->
                                autoDetect = checked
                                if (checked) preferredMode = LanguageMode.AUTO
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = GamingEmerald,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ElevatedSlate
                            )
                        )
                    }
                }
            }

            item {
                GamingCard(glowColor = BorderCyan) {
                    SectionHeader(
                        title = "SUPPORTED REPLY LANGUAGES",
                        subtitle = "Enable languages GR GAMER AI is allowed to reply in"
                    )
                    Spacer(Modifier.height(10.dp))

                    val languageSpecs = listOf(
                        Triple("HINDI", "Hindi (हिन्दी)", "Example: \"भाई OP गेमप्ले! ऐसे ही सपोर्ट बनाए रखो!\""),
                        Triple("HINGLISH", "Hinglish (Hindi + English)", "Example: \"Shukriya bhai! Next match mein Booyah pakka!\""),
                        Triple("ENGLISH", "English", "Example: \"Thanks bro! Glad you enjoyed the Free Fire clutch!\"")
                    )

                    languageSpecs.forEach { (code, title, example) ->
                        val checked = code in enabledLanguages
                        Surface(
                            onClick = {
                                val next = enabledLanguages.toMutableSet()
                                if (checked && next.size > 1) next.remove(code) else next.add(code)
                                enabledLanguages = next
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = ElevatedSlate,
                            border = BorderStroke(1.dp, if (checked) NeonCyan else BorderCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isNowChecked ->
                                        val next = enabledLanguages.toMutableSet()
                                        if (!isNowChecked && next.size > 1) next.remove(code)
                                        else if (isNowChecked) next.add(code)
                                        enabledLanguages = next
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NeonCyan,
                                        checkmarkColor = ObsidianBlack
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        LanguageBadge(code)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = example,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                GamingCard(glowColor = BorderCyan) {
                    SectionHeader(
                        title = "LANGUAGE PRIORITY MODE",
                        subtitle = "Choose auto-matching or lock replies to one language"
                    )
                    Spacer(Modifier.height(8.dp))
                    LanguageMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferredMode == mode,
                                onClick = {
                                    preferredMode = mode
                                    autoDetect = mode == LanguageMode.AUTO
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Column {
                                Text(mode.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onSaveLanguageSettings(
                            autoDetect,
                            preferredMode,
                            enabledLanguages.toList()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_language_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Text("Save Language Settings", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
