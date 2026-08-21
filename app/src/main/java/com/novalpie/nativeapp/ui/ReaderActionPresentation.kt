package com.novalpie.nativeapp.ui

/** Stable identifiers keep reader icon, label and callback selection in one vocabulary. */
internal enum class ReaderRailActionId {
    Close,
    Help,
    Catalog,
    Settings,
    Theme,
    Previous,
    Next,
    ReadingMode,
    Tts,
    Fullscreen,
    Navigation,
}

internal data class ReaderRailActionSpec(
    val id: ReaderRailActionId,
    val label: String,
)

internal fun readerRailActionSpecs(): List<ReaderRailActionSpec> = listOf(
    ReaderRailActionSpec(ReaderRailActionId.Close, "关闭"),
    ReaderRailActionSpec(ReaderRailActionId.Help, "帮助"),
    ReaderRailActionSpec(ReaderRailActionId.Catalog, "目录"),
    ReaderRailActionSpec(ReaderRailActionId.Settings, "设置"),
    ReaderRailActionSpec(ReaderRailActionId.Theme, "主题"),
    ReaderRailActionSpec(ReaderRailActionId.Previous, "上章"),
    ReaderRailActionSpec(ReaderRailActionId.Next, "下章"),
    ReaderRailActionSpec(ReaderRailActionId.ReadingMode, "滑动"),
    ReaderRailActionSpec(ReaderRailActionId.Tts, "听书"),
    ReaderRailActionSpec(ReaderRailActionId.Fullscreen, "全屏"),
    ReaderRailActionSpec(ReaderRailActionId.Navigation, "导航"),
)

internal fun readerRailActionEnabled(
    id: ReaderRailActionId,
    hasPrevious: Boolean,
    hasNext: Boolean,
    showTts: Boolean,
): Boolean = when (id) {
    ReaderRailActionId.Previous -> hasPrevious
    ReaderRailActionId.Next -> hasNext
    ReaderRailActionId.Tts -> showTts
    else -> true
}

internal fun readerRailActionSelected(
    id: ReaderRailActionId,
    selectedAction: ReaderRailActionId?,
): Boolean = id == selectedAction
