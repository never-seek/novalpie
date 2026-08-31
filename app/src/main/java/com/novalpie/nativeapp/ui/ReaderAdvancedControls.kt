package com.novalpie.nativeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.data.ReaderTtsSettingsStore
import com.novalpie.nativeapp.model.FavoriteStatus
import com.novalpie.nativeapp.model.LoadResult

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

@Composable
internal fun ReaderRadialMenu(
    state: ReaderState,
    chapters: List<com.novalpie.nativeapp.model.Chapter>,
    favoriteStatus: LoadResult<FavoriteStatus>,
    ttsState: ReaderTtsState,
    showTts: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCatalog: () -> Unit,
    onTts: () -> Unit,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorite = (favoriteStatus as? LoadResult.Success)?.value?.isFavorited == true
    Surface(
        modifier = modifier.padding(20.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("阅读工具", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("再次点击正文关闭", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderRadialAction(Icons.AutoMirrored.Filled.NavigateBefore, "上一章", onPrevious)
                ReaderRadialAction(Icons.AutoMirrored.Filled.MenuBook, "目录", onCatalog)
                ReaderRadialAction(Icons.AutoMirrored.Filled.NavigateNext, "下一章", onNext)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showTts) {
                    ReaderRadialAction(Icons.Filled.RecordVoiceOver, if (ttsState == ReaderTtsState.Speaking) "停止" else "听书", onTts)
                }
                ReaderRadialAction(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (favorite) "已收藏" else "收藏", onFavorite)
                FilterChip(selected = false, onClick = onDismiss, label = { Text("关闭") })
            }
        }
    }
}

@Composable
private fun ReaderRadialAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}
