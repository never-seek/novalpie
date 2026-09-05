package com.novalpie.nativeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.data.ReaderTtsSettingsStore

private val readerTtsLanguageChoices = listOf(
    "zh-CN" to "中文（简体）",
    "zh-TW" to "中文（繁体）",
    "en-US" to "English",
    "ja-JP" to "日本語",
    "ko-KR" to "한국어",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderTtsSettingsControls(
    settings: ReaderTtsSettings,
    textColor: Color,
    metaColor: Color,
    voiceOptions: List<ReaderTtsVoiceOption> = emptyList(),
    onChange: ((ReaderTtsSettings) -> ReaderTtsSettings) -> Unit,
) {
    ReaderSettingsSection(title = "听书设置", textColor = textColor, metaColor = metaColor) {
        ReaderValueSlider(
            label = "语速",
            valueLabel = String.format(java.util.Locale.US, "%.2f", settings.rate),
            value = settings.rate,
            valueRange = ReaderTtsSettingsStore.MIN_RATE..ReaderTtsSettingsStore.MAX_RATE,
            steps = 10,
            onValueChange = { value -> onChange { it.copy(rate = value) } },
        )
        ReaderValueSlider(
            label = "音调",
            valueLabel = String.format(java.util.Locale.US, "%.2f", settings.pitch),
            value = settings.pitch,
            valueRange = ReaderTtsSettingsStore.MIN_PITCH..ReaderTtsSettingsStore.MAX_PITCH,
            steps = 7,
            onValueChange = { value -> onChange { it.copy(pitch = value) } },
        )
        ReaderValueSlider(
            label = "音量",
            valueLabel = "${(settings.volume * 100).toInt()}%",
            value = settings.volume,
            valueRange = 0f..1f,
            steps = 9,
            onValueChange = { value -> onChange { it.copy(volume = value) } },
        )
        ReaderValueSlider(
            label = "段间停顿",
            valueLabel = "${settings.pauseBetweenSegmentsMs} ms",
            value = settings.pauseBetweenSegmentsMs.toFloat(),
            valueRange = 0f..ReaderTtsSettingsStore.MAX_PAUSE_MS.toFloat(),
            steps = 19,
            onValueChange = { value ->
                onChange { it.copy(pauseBetweenSegmentsMs = value.toInt().coerceIn(0, ReaderTtsSettingsStore.MAX_PAUSE_MS)) }
            },
        )
        Text("语言", style = MaterialTheme.typography.labelLarge, color = textColor)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            readerTtsLanguageChoices.forEach { (key, label) ->
                FilterChip(
                    selected = settings.language == key,
                    onClick = { onChange { it.copy(language = key, voice = null) } },
                    label = { Text(label) },
                )
            }
        }
        if (voiceOptions.isNotEmpty()) {
            Text("声音", style = MaterialTheme.typography.labelLarge, color = textColor)
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                voiceOptions
                    .filter { it.languageTag.startsWith(settings.language.substringBefore('-'), ignoreCase = true) }
                    .take(8)
                    .forEach { voice ->
                        FilterChip(
                            selected = settings.voice == voice.name,
                            onClick = { onChange { it.copy(voice = voice.name) } },
                            label = { Text(voice.label, maxLines = 1) },
                        )
                    }
            }
        }
        ReaderToggleRow("朗读后自动滚动", settings.enableAutoScroll, textColor, metaColor) {
            onChange { it.copy(enableAutoScroll = !it.enableAutoScroll) }
        }
        ReaderToggleRow("读完自动下一章", settings.enableAutoNextChapter, textColor, metaColor) {
            onChange { it.copy(enableAutoNextChapter = !it.enableAutoNextChapter) }
        }
        ReaderToggleRow("朗读高亮", settings.enableHighlight, textColor, metaColor) {
            onChange { it.copy(enableHighlight = !it.enableHighlight) }
        }
    }
}
