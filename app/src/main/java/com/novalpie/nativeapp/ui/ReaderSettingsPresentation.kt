package com.novalpie.nativeapp.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.data.ReaderFontStore
import com.novalpie.nativeapp.model.ReaderCustomTheme
import com.novalpie.nativeapp.model.ReaderTapArea
import com.novalpie.nativeapp.model.normalizeReaderCustomTheme
import com.novalpie.nativeapp.model.readerCustomThemeIdFromKey
import com.novalpie.nativeapp.model.readerCustomThemeKey
import java.util.UUID

private val readerFontChoices = listOf(
    "system" to "系统",
    "serif" to "衬线",
    "sans" to "无衬线",
    "monospace" to "等宽",
)

private val readerTapActions = listOf(
    "none" to "无操作",
    "pagePrev" to "上一章",
    "pageNext" to "下一章",
    "sidebar" to "显示工具栏",
    "catalog" to "打开目录",
)

/**
 * Mirrors the live reader sidebar's groups.  Keeping the groups explicit prevents a new setting
 * from silently turning the mobile sheet back into one long, hard-to-scan form.
 */
internal enum class ReaderSettingsCategory(val label: String, val summary: String) {
    Font("字体", "字号、字体和字重"),
    Typography("排版", "行高、缩进和间距"),
    Display("显示", "评论、插图和工具栏"),
    Layout("布局", "滚动、翻页和点击区域"),
    Replacement("替换", "正文替换模式"),
    Theme("主题", "阅读背景与颜色"),
    Tts("听书", "语速、声音和朗读行为"),
    Other("其他", "恢复本机阅读偏好"),
}

internal fun readerSettingsCategories(): List<ReaderSettingsCategory> =
    ReaderSettingsCategory.values().toList()

internal fun readerSettingsCategoryLabels(): List<String> =
    readerSettingsCategories().map { it.label }

/** Keeps the settings rail scannable when the reader has more than one kind of preference. */
internal fun readerSettingsCategoryIcon(category: ReaderSettingsCategory): ImageVector = when (category) {
    ReaderSettingsCategory.Font -> Icons.Filled.FormatSize
    ReaderSettingsCategory.Typography -> Icons.Filled.FormatLineSpacing
    ReaderSettingsCategory.Display -> Icons.Filled.Visibility
    ReaderSettingsCategory.Layout -> Icons.AutoMirrored.Filled.ViewQuilt
    ReaderSettingsCategory.Replacement -> Icons.Filled.FindReplace
    ReaderSettingsCategory.Theme -> Icons.Filled.LightMode
    ReaderSettingsCategory.Tts -> Icons.Filled.RecordVoiceOver
    ReaderSettingsCategory.Other -> Icons.Filled.Tune
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderSettingsCategoryRail(
    selected: ReaderSettingsCategory,
    onSelected: (ReaderSettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = readerSettingsCategories()
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        categories.indexOf(selected)
            .takeIf { it >= 0 }
            ?.let { index -> listState.scrollToItem(index) }
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(categories, key = { it.name }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelected(category) },
                label = { Text(category.label) },
                leadingIcon = {
                    Icon(
                        imageVector = readerSettingsCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderSettingsControls(
    options: ReaderUiOptions,
    category: ReaderSettingsCategory,
    textColor: Color,
    metaColor: Color,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onCycleTheme: () -> Unit,
    onOptionsChange: (ReaderUiOptions) -> Unit,
    onReset: () -> Unit,
    onClearCurrentBookCache: () -> Unit,
    clearingChapterCache: Boolean,
    chapterCacheMessage: String?,
) {
    val context = LocalContext.current
    var fontImportMessage by remember { mutableStateOf<String?>(null) }
    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        ReaderFontStore.import(context, uri).fold(
            onSuccess = { key ->
                onOptionsChange(options.copy(fontFamily = key).normalizedReaderOptions())
                fontImportMessage = "已导入：${ReaderFontStore.displayName(key).orEmpty()}"
            },
            onFailure = { failure ->
                fontImportMessage = failure.message ?: "字体导入失败"
            },
        )
    }

    fun update(transform: (ReaderUiOptions) -> ReaderUiOptions) {
        onOptionsChange(transform(options).normalizedReaderOptions())
    }

    if (category == ReaderSettingsCategory.Font) {
        ReaderSettingsSection(title = "字体设置", textColor = textColor, metaColor = metaColor) {
        ReaderValueSlider(
            label = "字号",
            valueLabel = "${options.fontSizeSp} sp",
            value = options.fontSizeSp.toFloat(),
            valueRange = ReaderSettingsStore.MIN_FONT_SIZE_SP.toFloat()..ReaderSettingsStore.MAX_FONT_SIZE_SP.toFloat(),
            steps = 35,
            onValueChange = { value -> update { options -> options.copy(fontSizeSp = value.roundToInt()) } },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onDecreaseFont) { Text("A-") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onIncreaseFont) { Text("A+") }
        }
        ReaderValueSlider(
            label = "字重",
            valueLabel = "${options.fontWeight}",
            value = options.fontWeight.toFloat(),
            valueRange = ReaderSettingsStore.MIN_FONT_WEIGHT.toFloat()..ReaderSettingsStore.MAX_FONT_WEIGHT.toFloat(),
            steps = 7,
            onValueChange = { value -> update { options -> options.copy(fontWeight = (value / 100f).roundToInt() * 100) } },
        )
        Text("字体", style = MaterialTheme.typography.labelLarge, color = textColor)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            readerFontChoices.forEach { (key, label) ->
                FilterChip(
                    selected = options.fontFamily == key,
                    onClick = { update { it.copy(fontFamily = key) } },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = ReaderFontStore.displayName(options.fontFamily) ?: "未选择自定义字体",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = metaColor,
                maxLines = 1,
            )
            OutlinedButton(
                onClick = {
                    fontPicker.launch(
                        arrayOf(
                            "font/ttf",
                            "font/otf",
                            "font/ttc",
                            "application/x-font-ttf",
                            "application/x-font-opentype",
                            "application/octet-stream",
                        )
                    )
                },
            ) { Text("导入字体") }
            if (ReaderFontStore.customFontFileName(options.fontFamily) != null) {
                TextButton(onClick = { update { it.copy(fontFamily = ReaderSettingsStore.DEFAULT_FONT_FAMILY) } }) {
                    Text("清除")
                }
            }
        }
        Text(
            text = fontImportMessage ?: "支持 TTF、OTF、TTC；字体会保存在本机阅读器设置中",
            style = MaterialTheme.typography.labelSmall,
            color = if (fontImportMessage != null) MaterialTheme.colorScheme.primary else metaColor,
        )
        }
    }

    if (category == ReaderSettingsCategory.Typography) {
        ReaderSettingsSection(title = "排版设置", textColor = textColor, metaColor = metaColor) {
        ReaderValueSlider(
            label = "行高",
            valueLabel = String.format(java.util.Locale.US, "%.1f 倍", options.lineHeight),
            value = options.lineHeight,
            valueRange = ReaderSettingsStore.MIN_LINE_HEIGHT..ReaderSettingsStore.MAX_LINE_HEIGHT,
            steps = 19,
            onValueChange = { value -> update { options -> options.copy(lineHeight = value.roundTo(1)) } },
        )
        ReaderValueSlider(
            label = "内容宽度",
            valueLabel = "${options.contentWidthDp} dp",
            value = options.contentWidthDp.toFloat(),
            valueRange = ReaderSettingsStore.MIN_CONTENT_WIDTH_DP.toFloat()..ReaderSettingsStore.MAX_CONTENT_WIDTH_DP.toFloat(),
            steps = 15,
            onValueChange = { value -> update { options -> options.copy(contentWidthDp = (value / 50f).roundToInt() * 50) } },
        )
        ReaderValueSlider(
            label = "字距",
            valueLabel = String.format(java.util.Locale.US, "%.1f sp", options.letterSpacing),
            value = options.letterSpacing,
            valueRange = ReaderSettingsStore.MIN_LETTER_SPACING..ReaderSettingsStore.MAX_LETTER_SPACING,
            steps = 13,
            onValueChange = { value -> update { options -> options.copy(letterSpacing = value.roundTo(1)) } },
        )
        ReaderValueSlider(
            label = "字符间距",
            valueLabel = String.format(java.util.Locale.US, "%.1f sp", options.wordSpacing),
            value = options.wordSpacing,
            valueRange = ReaderSettingsStore.MIN_WORD_SPACING..ReaderSettingsStore.MAX_WORD_SPACING,
            steps = 23,
            onValueChange = { value -> update { options -> options.copy(wordSpacing = value.roundTo(1)) } },
        )
        ReaderToggleRow("首行缩进", options.textIndent, textColor, metaColor) {
            update { it.copy(textIndent = !it.textIndent) }
        }
        ReaderToggleRow("段落间空行", options.emptyLine, textColor, metaColor) {
            update { it.copy(emptyLine = !it.emptyLine) }
        }
        ReaderToggleRow("清理重复行", options.removeDuplicateLines, textColor, metaColor) {
            update { it.copy(removeDuplicateLines = !it.removeDuplicateLines) }
        }
        }
    }

    if (category == ReaderSettingsCategory.Display) {
        ReaderSettingsSection(title = "显示设置", textColor = textColor, metaColor = metaColor) {
        ReaderToggleRow("显示章节评论", options.showComments, textColor, metaColor) {
            update { it.copy(showComments = !it.showComments) }
        }
        ReaderToggleRow("显示正文插图", options.showImages, textColor, metaColor) {
            update { it.copy(showImages = !it.showImages) }
        }
        ReaderToggleRow("显示顶部栏", options.showHeader, textColor, metaColor) {
            update { it.copy(showHeader = !it.showHeader) }
        }
        ReaderToggleRow("显示底部栏", options.showFooter, textColor, metaColor) {
            update { it.copy(showFooter = !it.showFooter) }
        }
        ReaderToggleRow("显示收藏按钮", options.showFavoriteButton, textColor, metaColor) {
            update { it.copy(showFavoriteButton = !it.showFavoriteButton) }
        }
        ReaderToggleRow("显示听书入口", options.showTts, textColor, metaColor) {
            update { it.copy(showTts = !it.showTts) }
        }
        ReaderToggleRow("显示轮盘菜单", options.showRadialMenu, textColor, metaColor) {
            update { it.copy(showRadialMenu = !it.showRadialMenu) }
        }
        Text("轮盘菜单触发方式", style = MaterialTheme.typography.labelLarge, color = textColor)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("doubleTap" to "双击", "longPress" to "长按").forEach { (key, label) ->
                FilterChip(
                    selected = options.radialMenuOpenMode == key,
                    onClick = { update { it.copy(radialMenuOpenMode = key) } },
                    label = { Text(label) },
                )
            }
        }
        }
    }

    if (category == ReaderSettingsCategory.Replacement) {
        ReaderSettingsSection(title = "文本替换", textColor = textColor, metaColor = metaColor) {
        Text(
            "正文重新加载后生效；无替换模式受账号资格限制时会按网页规则回退到印度模式。",
            style = MaterialTheme.typography.bodySmall,
            color = metaColor,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            readerReplaceModeOptions().forEach { (key, label) ->
                FilterChip(
                    selected = options.replaceMode == key,
                    onClick = { update { it.copy(replaceMode = key) } },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        Text(
            "当前：${options.replaceMode.readerReplaceModeLabel()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        }
    }

    if (category == ReaderSettingsCategory.Theme) {
        ReaderSettingsSection(title = "阅读主题", textColor = textColor, metaColor = metaColor) {
        ReaderCustomThemeControls(
            options = options,
            textColor = textColor,
            metaColor = metaColor,
            onOptionsChange = onOptionsChange,
            onCycleTheme = onCycleTheme,
        )
        }
    }

    if (category == ReaderSettingsCategory.Layout) {
        ReaderSettingsSection(title = "布局与交互", textColor = textColor, metaColor = metaColor) {
        Text(
            "无限滚动与翻页模式只能选择一种；切换时会自动关闭另一种。",
            style = MaterialTheme.typography.bodySmall,
            color = metaColor,
        )
        ReaderToggleRow("无限滚动", options.useInfiniteScroll, textColor, metaColor) {
            update { current ->
                val enabled = !current.useInfiniteScroll
                current.copy(
                    useInfiniteScroll = enabled,
                    pageTurnMode = if (enabled) false else current.pageTurnMode,
                )
            }
        }
        ReaderToggleRow("翻页模式", options.pageTurnMode, textColor, metaColor) {
            update { current ->
                val enabled = !current.pageTurnMode
                current.copy(
                    pageTurnMode = enabled,
                    useInfiniteScroll = if (enabled) false else current.useInfiniteScroll,
                )
            }
        }
        Text(
            "翻页效果${if (options.pageTurnMode) "" else "（开启翻页模式后生效）"}",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "none" to "无动画",
                "fade" to "淡入",
                "cover" to "覆盖",
                "slide" to "滑动",
                "simulated" to "仿真",
            ).forEach { (key, label) ->
                FilterChip(
                    selected = options.pageTurnEffect == key,
                    enabled = options.pageTurnMode,
                    onClick = { update { it.copy(pageTurnEffect = key) } },
                    label = { Text(label) },
                )
            }
        }
        ReaderValueSlider(
            label = "顶部留白",
            valueLabel = "${options.screenPaddingTopDp} dp",
            value = options.screenPaddingTopDp.toFloat(),
            valueRange = 0f..ReaderSettingsStore.MAX_SCREEN_PADDING_DP.toFloat(),
            steps = 19,
            onValueChange = { value -> update { options -> options.copy(screenPaddingTopDp = (value / 5f).roundToInt() * 5) } },
        )
        ReaderValueSlider(
            label = "底部留白",
            valueLabel = "${options.screenPaddingBottomDp} dp",
            value = options.screenPaddingBottomDp.toFloat(),
            valueRange = 0f..ReaderSettingsStore.MAX_SCREEN_PADDING_DP.toFloat(),
            steps = 19,
            onValueChange = { value -> update { options -> options.copy(screenPaddingBottomDp = (value / 5f).roundToInt() * 5) } },
        )
        Text("依次点击下方按钮循环设置左、中、右区域动作。", style = MaterialTheme.typography.bodySmall, color = metaColor)
        val areas = options.tapAreas.ifEmpty { defaultReaderTapAreas() }
        listOf("left" to "左侧", "center" to "中央", "right" to "右侧").forEach { (position, label) ->
            val area = areas.firstOrNull { it.position == position } ?: ReaderTapArea(position, "30%", "none")
            val currentIndex = readerTapActions.indexOfFirst { it.first == area.action }.coerceAtLeast(0)
            val nextAction = readerTapActions[(currentIndex + 1) % readerTapActions.size]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, modifier = Modifier.weight(1f), color = textColor)
                OutlinedButton(
                    onClick = {
                        update {
                            it.copy(tapAreas = areas.map { candidate ->
                                if (candidate.position == position) candidate.copy(action = nextAction.first) else candidate
                            })
                        }
                    },
                ) { Text(readerTapActions[currentIndex].second) }
            }
        }
        }
    }

    if (category == ReaderSettingsCategory.Other) {
        ReaderSettingsSection(title = "其他设置", textColor = textColor, metaColor = metaColor) {
            Text(
                "阅读偏好只保存在当前设备。恢复默认不会影响网站账号、收藏、下载或阅读进度。",
                style = MaterialTheme.typography.bodySmall,
                color = metaColor,
            )
            Text(
                "离线缓存仅保存本书已打开的章节；清除后会保留当前正在看的内存正文。",
                style = MaterialTheme.typography.bodySmall,
                color = metaColor,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = onClearCurrentBookCache,
                    enabled = !clearingChapterCache,
                ) {
                    Text(if (clearingChapterCache) "正在清除…" else "清除本书离线缓存")
                }
                OutlinedButton(onClick = onReset) { Text("恢复默认设置") }
            }
            chapterCacheMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (message.contains("失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderCustomThemeControls(
    options: ReaderUiOptions,
    textColor: Color,
    metaColor: Color,
    onOptionsChange: (ReaderUiOptions) -> Unit,
    onCycleTheme: () -> Unit,
) {
    val context = LocalContext.current
    val selectedId = readerCustomThemeIdFromKey(options.theme)
    val selectedTheme = options.customThemes.firstOrNull { it.id == selectedId }
    var draft by remember(selectedTheme?.id) { mutableStateOf(selectedTheme) }
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        draft = draft?.copy(backgroundImageUri = uri.toString())
    }

    fun selectPreset(key: String) {
        onOptionsChange(options.copy(theme = key))
    }

    fun createTheme() {
        val id = "theme-${UUID.randomUUID()}"
        val theme = ReaderCustomTheme(
            id = id,
            name = "自定义主题 ${options.customThemes.size + 1}",
        )
        onOptionsChange(
            options.copy(
                customThemes = (options.customThemes + theme).take(12),
                theme = readerCustomThemeKey(id),
            ),
        )
    }

    fun saveDraft() {
        val current = draft ?: return
        val normalized = normalizeReaderCustomTheme(current) ?: return
        onOptionsChange(
            options.copy(
                customThemes = options.customThemes.map { if (it.id == normalized.id) normalized else it },
                theme = readerCustomThemeKey(normalized.id),
            ),
        )
        draft = normalized
    }

    fun deleteSelectedTheme() {
        val id = selectedId ?: return
        onOptionsChange(
            options.copy(
                customThemes = options.customThemes.filterNot { it.id == id },
                theme = "system",
            ),
        )
        draft = null
    }

    Text(
        "预设主题会立即应用；自定义主题可保存正文、侧栏颜色和本地背景图。",
        style = MaterialTheme.typography.bodySmall,
        color = metaColor,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        readerThemeOptions().forEach { (key, label) ->
            FilterChip(
                selected = options.theme == key,
                onClick = { selectPreset(key) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
    if (options.customThemes.isNotEmpty()) {
        Text("我的自定义主题", style = MaterialTheme.typography.labelLarge, color = textColor)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.customThemes.forEach { theme ->
                FilterChip(
                    selected = theme.id == selectedId,
                    onClick = {
                        onOptionsChange(options.copy(theme = readerCustomThemeKey(theme.id)))
                    },
                    label = { Text(theme.name, maxLines = 1) },
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        OutlinedButton(onClick = onCycleTheme) { Text("循环预设") }
        Button(onClick = ::createTheme, enabled = options.customThemes.size < 12) {
            Text("新建自定义主题")
        }
    }

    if (draft != null && selectedId != null) {
        val current = draft!!
        Text(
            "编辑自定义主题",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
        OutlinedTextField(
            value = current.name,
            onValueChange = { draft = current.copy(name = it.take(48)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("主题名称") },
        )
        ReaderThemeHexField("正文背景色", current.backgroundHex, textColor) {
            draft = current.copy(backgroundHex = it.take(7))
        }
        ReaderThemeHexField("正文文字色", current.textHex, textColor) {
            draft = current.copy(textHex = it.take(7))
        }
        ReaderThemeHexField("侧栏背景色", current.sidebarBackgroundHex, textColor) {
            draft = current.copy(sidebarBackgroundHex = it.take(7))
        }
        ReaderThemeHexField("侧栏文字色", current.sidebarTextHex, textColor) {
            draft = current.copy(sidebarTextHex = it.take(7))
        }
        ReaderThemeHexField("强调色", current.accentHex, textColor) {
            draft = current.copy(accentHex = it.take(7))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = current.backgroundImageUri?.let { "已选择背景图" } ?: "未选择背景图",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = metaColor,
            )
            OutlinedButton(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) {
                Text("选择背景图")
            }
            if (current.backgroundImageUri != null) {
                TextButton(onClick = { draft = current.copy(backgroundImageUri = null) }) {
                    Text("移除")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = ::deleteSelectedTheme) { Text("删除主题") }
            Button(onClick = ::saveDraft) { Text("保存主题") }
        }
    }
}

@Composable
private fun ReaderThemeHexField(
    label: String,
    value: String,
    textColor: Color,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        supportingText = { Text("格式：#RRGGBB", color = textColor.copy(alpha = 0.65f)) },
    )
}

@Composable
internal fun ReaderSettingsSection(
    title: String,
    textColor: Color,
    metaColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = textColor)
        Text("本机阅读偏好", style = MaterialTheme.typography.labelSmall, color = metaColor)
        content()
    }
}

@Composable
internal fun ReaderValueSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(valueRange.start, valueRange.endInclusive), onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
internal fun ReaderToggleRow(
    label: String,
    checked: Boolean,
    textColor: Color,
    metaColor: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = textColor)
            Text("立即应用", style = MaterialTheme.typography.labelSmall, color = metaColor)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun Float.roundTo(decimals: Int): Float {
    val scale = 10f.pow(decimals)
    return kotlin.math.round(this * scale) / scale
}

private fun Float.pow(exponent: Int): Float = Math.pow(toDouble(), exponent.toDouble()).toFloat()

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
