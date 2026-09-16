package com.novalpie.nativeapp.feature.reader.preferences

import com.novalpie.nativeapp.ui.ReaderUiOptions
import com.novalpie.nativeapp.ui.normalizedReaderOptions
import com.novalpie.nativeapp.model.ReaderTapArea
import org.json.JSONArray
import org.json.JSONObject

internal object ReaderPreferenceCodec {
    private val fonts = mapOf("system" to "system-ui, -apple-system, sans-serif", "serif" to "serif", "sans" to "sans-serif", "monospace" to "monospace")
    private val themes = setOf("light", "dark", "sepia", "green", "gray")
    fun encode(options: ReaderUiOptions): JSONObject = JSONObject().apply {
        put("fontSize", options.fontSizeSp); put("lineHeight", options.lineHeight)
        fonts[options.fontFamily]?.let { put("fontFamily", it) }
        put("fontWeight", options.fontWeight); put("letterSpacing", options.letterSpacing); put("wordSpacing", options.wordSpacing)
        if (options.theme in themes) put("theme", "theme-${options.theme}")
        put("emptyLine", options.emptyLine); put("textIndent", options.textIndent); put("removeDuplicateLines", options.removeDuplicateLines)
        put("showComments", options.showComments); put("showImages", options.showImages); put("showTTS", options.showTts)
        put("showHeader", options.showHeader); put("showFooter", options.showFooter); put("showFavoriteButton", options.showFavoriteButton)
        put("screenPadding", JSONObject().put("top", options.screenPaddingTopDp).put("bottom", options.screenPaddingBottomDp))
        put("contentWidth", options.contentWidthDp); put("replaceMode", options.replaceMode)
        put("pageTurnMode", options.pageTurnMode); put("useInfiniteScroll", options.useInfiniteScroll)
        if (options.pageTurnEffect != "none") put("pageTurnEffect", options.pageTurnEffect)
        put("tapAreas", JSONArray().apply { options.tapAreas.forEach { area -> put(JSONObject().put("position", area.position).put("width", area.width).put("action", area.action)) } })
    }
    fun merge(preferences: JSONObject, current: ReaderUiOptions): ReaderUiOptions {
        fun number(key: String, default: Float): Float = preferences.optDouble(key, default.toDouble()).toFloat().takeIf(Float::isFinite) ?: default
        fun bool(key: String, default: Boolean): Boolean = if (preferences.isNull(key)) default else preferences.optBoolean(key, default)
        val page = bool("pageTurnMode", current.pageTurnMode)
        val theme = preferences.optString("theme").removePrefix("theme-").takeIf { it in themes } ?: current.theme
        val family = preferences.optString("fontFamily").let { source -> fonts.entries.firstOrNull { it.value == source || it.key == source }?.key } ?: current.fontFamily
        val padding = preferences.optJSONObject("screenPadding")
        val areas = preferences.optJSONArray("tapAreas")?.let { array -> (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { item -> ReaderTapArea(item.optString("position"), item.optString("width"), item.optString("action")) }
        } }?.takeIf { it.size == 3 && it.map { a -> a.position }.toSet() == setOf("left", "center", "right") &&
            it.all { a -> a.action in setOf("none", "pagePrev", "pageNext", "sidebar", "catalog") && a.width.matches(Regex("\\d+(?:\\.\\d+)?%")) } } ?: current.tapAreas
        return current.copy(fontSizeSp = number("fontSize", current.fontSizeSp.toFloat()).toInt(), lineHeight = number("lineHeight", current.lineHeight),
            fontFamily = family, fontWeight = number("fontWeight", current.fontWeight.toFloat()).toInt(),
            letterSpacing = number("letterSpacing", current.letterSpacing), wordSpacing = number("wordSpacing", current.wordSpacing), theme = theme,
            emptyLine = bool("emptyLine", current.emptyLine), textIndent = bool("textIndent", current.textIndent), removeDuplicateLines = bool("removeDuplicateLines", current.removeDuplicateLines),
            showComments = bool("showComments", current.showComments), showImages = bool("showImages", current.showImages), showTts = bool("showTTS", current.showTts),
            showHeader = bool("showHeader", current.showHeader), showFooter = bool("showFooter", current.showFooter), showFavoriteButton = bool("showFavoriteButton", current.showFavoriteButton),
            showRadialMenu = false, screenPaddingTopDp = padding?.optInt("top", current.screenPaddingTopDp) ?: current.screenPaddingTopDp,
            screenPaddingBottomDp = padding?.optInt("bottom", current.screenPaddingBottomDp) ?: current.screenPaddingBottomDp,
            contentWidthDp = number("contentWidth", current.contentWidthDp.toFloat()).toInt(), replaceMode = preferences.optString("replaceMode", current.replaceMode),
            pageTurnMode = page, useInfiniteScroll = !page && bool("useInfiniteScroll", current.useInfiniteScroll),
            pageTurnEffect = if (current.pageTurnEffect == "none") "none" else preferences.optString("pageTurnEffect", current.pageTurnEffect),
            tapAreas = areas).normalizedReaderOptions()
    }
}
