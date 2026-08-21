package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.ReaderTapArea
import com.novalpie.nativeapp.model.ReaderCustomTheme
import com.novalpie.nativeapp.model.normalizeReaderCustomThemes
import com.novalpie.nativeapp.model.readerCustomThemeIdFromKey
import org.json.JSONArray
import org.json.JSONObject

data class ReaderSettingsValues(
    val fontSizeSp: Int = ReaderSettingsStore.DEFAULT_FONT_SIZE_SP,
    val lineHeight: Float = ReaderSettingsStore.DEFAULT_LINE_HEIGHT,
    val fontFamily: String = ReaderSettingsStore.DEFAULT_FONT_FAMILY,
    val fontWeight: Int = ReaderSettingsStore.DEFAULT_FONT_WEIGHT,
    val letterSpacing: Float = ReaderSettingsStore.DEFAULT_LETTER_SPACING,
    val wordSpacing: Float = ReaderSettingsStore.DEFAULT_WORD_SPACING,
    val theme: String = ReaderSettingsStore.DEFAULT_THEME,
    val customThemes: List<ReaderCustomTheme> = emptyList(),
    val emptyLine: Boolean = true,
    val textIndent: Boolean = true,
    val removeDuplicateLines: Boolean = false,
    val showComments: Boolean = true,
    val showImages: Boolean = true,
    val showTts: Boolean = true,
    val showRadialMenu: Boolean = false,
    val radialMenuOpenMode: String = "doubleTap",
    val showHeader: Boolean = true,
    val showFooter: Boolean = true,
    val showFavoriteButton: Boolean = true,
    val screenPaddingTopDp: Int = 0,
    val screenPaddingBottomDp: Int = 0,
    val contentWidthDp: Int = ReaderSettingsStore.DEFAULT_CONTENT_WIDTH_DP,
    val replaceMode: String = ReaderSettingsStore.DEFAULT_REPLACE_MODE,
    val useInfiniteScroll: Boolean = true,
    val pageTurnMode: Boolean = false,
    val pageTurnEffect: String = "fade",
    val tapAreas: List<ReaderTapArea> = emptyList(),
)

class ReaderSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadFontSizeSp(): Int {
        return prefs.getInt(KEY_FONT_SIZE_SP, DEFAULT_FONT_SIZE_SP).coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
    }

    fun loadTheme(): String {
        val value = prefs.getString(KEY_THEME, DEFAULT_THEME).orEmpty()
        return normalizeTheme(value, loadCustomThemes())
    }

    fun saveFontSizeSp(value: Int) {
        prefs.edit().putInt(KEY_FONT_SIZE_SP, value.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)).apply()
    }

    fun saveTheme(value: String) {
        prefs.edit().putString(KEY_THEME, if (value in SUPPORTED_THEMES) value else DEFAULT_THEME).apply()
    }

    fun load(): ReaderSettingsValues {
        val defaultTapAreas = defaultTapAreas()
        val customThemes = loadCustomThemes()
        val savedPageTurnMode = prefs.getBoolean(KEY_PAGE_TURN_MODE, false)
        val showRadialMenu = migrateLegacyDefaultRadialMenuPreference()
        val showTts = migrateLegacyTtsVisibilityPreference()
        // Builds predating the continuous-scroll switch only persisted page-turn mode. Preserve
        // that user's explicit choice instead of treating a missing new key as "enabled".
        val useInfiniteScroll = if (prefs.contains(KEY_USE_INFINITE_SCROLL)) {
            prefs.getBoolean(KEY_USE_INFINITE_SCROLL, true)
        } else {
            !savedPageTurnMode
        }
        return ReaderSettingsValues(
            fontSizeSp = loadFontSizeSp(),
            lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, DEFAULT_LINE_HEIGHT).coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT),
            fontFamily = prefs.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY).orEmpty()
                .takeIf { it in SUPPORTED_FONT_FAMILIES } ?: DEFAULT_FONT_FAMILY,
            fontWeight = prefs.getInt(KEY_FONT_WEIGHT, DEFAULT_FONT_WEIGHT).coerceIn(MIN_FONT_WEIGHT, MAX_FONT_WEIGHT),
            letterSpacing = prefs.getFloat(KEY_LETTER_SPACING, DEFAULT_LETTER_SPACING).coerceIn(MIN_LETTER_SPACING, MAX_LETTER_SPACING),
            wordSpacing = prefs.getFloat(KEY_WORD_SPACING, DEFAULT_WORD_SPACING).coerceIn(MIN_WORD_SPACING, MAX_WORD_SPACING),
            theme = normalizeTheme(prefs.getString(KEY_THEME, DEFAULT_THEME).orEmpty(), customThemes),
            customThemes = customThemes,
            emptyLine = prefs.getBoolean(KEY_EMPTY_LINE, true),
            textIndent = prefs.getBoolean(KEY_TEXT_INDENT, true),
            removeDuplicateLines = prefs.getBoolean(KEY_REMOVE_DUPLICATE_LINES, false),
            showComments = prefs.getBoolean(KEY_SHOW_COMMENTS, true),
            showImages = prefs.getBoolean(KEY_SHOW_IMAGES, true),
            showTts = showTts,
            // The source reader opens its chrome from a deliberate normal tap. Keep the optional
            // radial menu opt-in so a fresh native install does not require a surprising double
            // tap before Catalog, Settings, or Back become visible.
            showRadialMenu = showRadialMenu,
            radialMenuOpenMode = prefs.getString(KEY_RADIAL_MENU_OPEN_MODE, "doubleTap")
                .orEmpty().takeIf { it in SUPPORTED_RADIAL_MODES } ?: "doubleTap",
            showHeader = prefs.getBoolean(KEY_SHOW_HEADER, true),
            showFooter = prefs.getBoolean(KEY_SHOW_FOOTER, true),
            showFavoriteButton = prefs.getBoolean(KEY_SHOW_FAVORITE_BUTTON, true),
            screenPaddingTopDp = prefs.getInt(KEY_SCREEN_PADDING_TOP, 0).coerceIn(0, MAX_SCREEN_PADDING_DP),
            screenPaddingBottomDp = prefs.getInt(KEY_SCREEN_PADDING_BOTTOM, 0).coerceIn(0, MAX_SCREEN_PADDING_DP),
            contentWidthDp = prefs.getInt(KEY_CONTENT_WIDTH, DEFAULT_CONTENT_WIDTH_DP)
                .coerceIn(MIN_CONTENT_WIDTH_DP, MAX_CONTENT_WIDTH_DP),
            replaceMode = prefs.getString(KEY_REPLACE_MODE, DEFAULT_REPLACE_MODE).orEmpty()
                .takeIf { it in SUPPORTED_REPLACE_MODES } ?: DEFAULT_REPLACE_MODE,
            // Continuous scroll and page-turning are two mutually exclusive reading modes. Older
            // builds persisted them independently, which could leave the UI saying continuous
            // scroll was enabled while the reader had disabled its LazyColumn scrolling.
            useInfiniteScroll = useInfiniteScroll,
            pageTurnMode = savedPageTurnMode && !useInfiniteScroll,
            pageTurnEffect = prefs.getString(KEY_PAGE_TURN_EFFECT, "fade")
                .orEmpty().takeIf { it in SUPPORTED_PAGE_TURN_EFFECTS } ?: "fade",
            tapAreas = loadTapAreas(defaultTapAreas),
        )
    }

    fun save(values: ReaderSettingsValues) {
        prefs.edit()
            .putInt(KEY_FONT_SIZE_SP, values.fontSizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP))
            .putFloat(KEY_LINE_HEIGHT, values.lineHeight.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT))
            .putString(KEY_FONT_FAMILY, values.fontFamily.takeIf { it in SUPPORTED_FONT_FAMILIES } ?: DEFAULT_FONT_FAMILY)
            .putInt(KEY_FONT_WEIGHT, values.fontWeight.coerceIn(MIN_FONT_WEIGHT, MAX_FONT_WEIGHT))
            .putFloat(KEY_LETTER_SPACING, values.letterSpacing.coerceIn(MIN_LETTER_SPACING, MAX_LETTER_SPACING))
            .putFloat(KEY_WORD_SPACING, values.wordSpacing.coerceIn(MIN_WORD_SPACING, MAX_WORD_SPACING))
            .putString(
                KEY_THEME,
                normalizeTheme(values.theme, normalizeReaderCustomThemes(values.customThemes)),
            )
            .putString(KEY_CUSTOM_THEMES, encodeCustomThemes(values.customThemes))
            .putBoolean(KEY_EMPTY_LINE, values.emptyLine)
            .putBoolean(KEY_TEXT_INDENT, values.textIndent)
            .putBoolean(KEY_REMOVE_DUPLICATE_LINES, values.removeDuplicateLines)
            .putBoolean(KEY_SHOW_COMMENTS, values.showComments)
            .putBoolean(KEY_SHOW_IMAGES, values.showImages)
            .putBoolean(KEY_SHOW_TTS, values.showTts)
            .putBoolean(KEY_SHOW_RADIAL_MENU, values.showRadialMenu)
            .putString(KEY_RADIAL_MENU_OPEN_MODE, values.radialMenuOpenMode.takeIf { it in SUPPORTED_RADIAL_MODES } ?: "doubleTap")
            .putBoolean(KEY_SHOW_HEADER, values.showHeader)
            .putBoolean(KEY_SHOW_FOOTER, values.showFooter)
            .putBoolean(KEY_SHOW_FAVORITE_BUTTON, values.showFavoriteButton)
            .putInt(KEY_SCREEN_PADDING_TOP, values.screenPaddingTopDp.coerceIn(0, MAX_SCREEN_PADDING_DP))
            .putInt(KEY_SCREEN_PADDING_BOTTOM, values.screenPaddingBottomDp.coerceIn(0, MAX_SCREEN_PADDING_DP))
            .putInt(KEY_CONTENT_WIDTH, values.contentWidthDp.coerceIn(MIN_CONTENT_WIDTH_DP, MAX_CONTENT_WIDTH_DP))
            .putString(KEY_REPLACE_MODE, values.replaceMode.takeIf { it in SUPPORTED_REPLACE_MODES } ?: DEFAULT_REPLACE_MODE)
            .putBoolean(KEY_USE_INFINITE_SCROLL, values.useInfiniteScroll)
            .putBoolean(KEY_PAGE_TURN_MODE, values.pageTurnMode && !values.useInfiniteScroll)
            .putString(KEY_PAGE_TURN_EFFECT, values.pageTurnEffect.takeIf { it in SUPPORTED_PAGE_TURN_EFFECTS } ?: "fade")
            .putString(KEY_TAP_AREAS, encodeTapAreas(values.tapAreas))
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    private fun loadTapAreas(fallback: List<ReaderTapArea>): List<ReaderTapArea> {
        val raw = prefs.getString(KEY_TAP_AREAS, null).orEmpty()
        if (raw.isBlank()) return fallback
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                val position = item.optString("position").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val width = item.optString("width").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val action = item.optString("action").takeIf(String::isNotBlank) ?: return@mapNotNull null
                ReaderTapArea(position, width, action)
            }.takeIf { it.size == 3 } ?: fallback
        }.getOrDefault(fallback)
    }

    /**
     * Older native builds saved an enabled double-tap radial panel as their factory default.  That
     * made a normal reader action invisible after an upgrade, even though it was never a deliberate
     * setting for most people.  Migrate that exact legacy default once, while preserving the old
     * long-press choice as an intentional custom gesture.
     */
    private fun migrateLegacyDefaultRadialMenuPreference(): Boolean {
        val currentVersion = prefs.getInt(KEY_READER_CHROME_GESTURE_VERSION, 0)
        val savedRadialMenu = prefs.getBoolean(KEY_SHOW_RADIAL_MENU, false)
        val savedOpenMode = prefs.getString(KEY_RADIAL_MENU_OPEN_MODE, "doubleTap").orEmpty()
            .takeIf { it in SUPPORTED_RADIAL_MODES } ?: "doubleTap"
        if (currentVersion >= READER_CHROME_GESTURE_VERSION) return savedRadialMenu

        val migratedRadialMenu = savedRadialMenu && savedOpenMode != "doubleTap"
        prefs.edit()
            .putBoolean(KEY_SHOW_RADIAL_MENU, migratedRadialMenu)
            .putInt(KEY_READER_CHROME_GESTURE_VERSION, READER_CHROME_GESTURE_VERSION)
            .apply()
        return migratedRadialMenu
    }

    /**
     * Early native builds could leave the toolbar's primary TTS entry disabled after an upgrade.
     * Restore the source reader default once; subsequent explicit user choices remain untouched.
     */
    private fun migrateLegacyTtsVisibilityPreference(): Boolean {
        val currentVersion = prefs.getInt(KEY_READER_TTS_VISIBILITY_VERSION, 0)
        val savedVisible = prefs.getBoolean(KEY_SHOW_TTS, true)
        if (currentVersion >= READER_TTS_VISIBILITY_VERSION) return savedVisible
        prefs.edit()
            .putBoolean(KEY_SHOW_TTS, true)
            .putInt(KEY_READER_TTS_VISIBILITY_VERSION, READER_TTS_VISIBILITY_VERSION)
            .apply()
        return true
    }

    private fun encodeTapAreas(areas: List<ReaderTapArea>): String = JSONArray().apply {
        areas.take(3).forEach { area ->
            put(JSONObject().put("position", area.position).put("width", area.width).put("action", area.action))
        }
    }.toString()

    private fun loadCustomThemes(): List<ReaderCustomTheme> {
        val raw = prefs.getString(KEY_CUSTOM_THEMES, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            normalizeReaderCustomThemes(
                (0 until json.length()).mapNotNull { index ->
                    val item = json.optJSONObject(index) ?: return@mapNotNull null
                    ReaderCustomTheme(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        backgroundHex = item.optString("backgroundHex"),
                        textHex = item.optString("textHex"),
                        sidebarBackgroundHex = item.optString("sidebarBackgroundHex"),
                        sidebarTextHex = item.optString("sidebarTextHex"),
                        accentHex = item.optString("accentHex"),
                        backgroundImageUri = item.optString("backgroundImageUri")
                            .takeIf(String::isNotBlank),
                    )
                },
            )
        }.getOrDefault(emptyList())
    }

    private fun encodeCustomThemes(themes: List<ReaderCustomTheme>): String = JSONArray().apply {
        normalizeReaderCustomThemes(themes).forEach { theme ->
            put(
                JSONObject()
                    .put("id", theme.id)
                    .put("name", theme.name)
                    .put("backgroundHex", theme.backgroundHex)
                    .put("textHex", theme.textHex)
                    .put("sidebarBackgroundHex", theme.sidebarBackgroundHex)
                    .put("sidebarTextHex", theme.sidebarTextHex)
                    .put("accentHex", theme.accentHex)
                    .put("backgroundImageUri", theme.backgroundImageUri ?: ""),
            )
        }
    }.toString()

    private fun normalizeTheme(value: String, customThemes: List<ReaderCustomTheme>): String {
        if (value in SUPPORTED_THEMES) return value
        val customId = readerCustomThemeIdFromKey(value)
        return if (customId != null && customThemes.any { it.id == customId }) value else DEFAULT_THEME
    }

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_reader_settings"
        // The website exposes the full 12-48 range. Keep the native reader in the same range so
        // an accessibility-size preference is not silently clamped when it is moved between them.
        const val MIN_FONT_SIZE_SP = 12
        const val MAX_FONT_SIZE_SP = 48
        // Matches the current website reader for people who have not picked a size yet.
        // Existing saved values remain untouched when this default changes.
        const val DEFAULT_FONT_SIZE_SP = 16
        const val DEFAULT_THEME = "system"
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 3.0f
        const val DEFAULT_LINE_HEIGHT = 1.6f
        const val DEFAULT_FONT_FAMILY = "system"
        const val DEFAULT_FONT_WEIGHT = 400
        const val MIN_FONT_WEIGHT = 100
        const val MAX_FONT_WEIGHT = 900
        const val DEFAULT_LETTER_SPACING = 0f
        const val MIN_LETTER_SPACING = -2f
        const val MAX_LETTER_SPACING = 5f
        const val DEFAULT_WORD_SPACING = 0f
        const val MIN_WORD_SPACING = -2f
        const val MAX_WORD_SPACING = 10f
        const val DEFAULT_CONTENT_WIDTH_DP = 800
        const val MIN_CONTENT_WIDTH_DP = 400
        const val MAX_CONTENT_WIDTH_DP = 1200
        const val MAX_SCREEN_PADDING_DP = 100
        const val DEFAULT_REPLACE_MODE = "india"
        private val SUPPORTED_THEMES = setOf("system", "light", "sepia", "dark", "green", "gray", "high_contrast")
        private val SUPPORTED_FONT_FAMILIES = setOf("system", "serif", "sans", "monospace")
        private val SUPPORTED_RADIAL_MODES = setOf("doubleTap", "longPress")
        private val SUPPORTED_PAGE_TURN_EFFECTS = setOf("fade", "cover", "slide", "simulated")
        private val SUPPORTED_REPLACE_MODES = setOf(
            "", "korea", "india", "europe", "usa", "hyrule", "azeroth", "tamriel",
            "middle_earth", "terra", "genshin"
        )
        private const val KEY_FONT_SIZE_SP = "font_size_sp"
        private const val KEY_THEME = "theme"
        private const val KEY_LINE_HEIGHT = "line_height"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_WEIGHT = "font_weight"
        private const val KEY_LETTER_SPACING = "letter_spacing"
        private const val KEY_WORD_SPACING = "word_spacing"
        private const val KEY_EMPTY_LINE = "empty_line"
        private const val KEY_CUSTOM_THEMES = "custom_themes"
        private const val KEY_TEXT_INDENT = "text_indent"
        private const val KEY_REMOVE_DUPLICATE_LINES = "remove_duplicate_lines"
        private const val KEY_SHOW_COMMENTS = "show_comments"
        private const val KEY_SHOW_IMAGES = "show_images"
        private const val KEY_SHOW_TTS = "show_tts"
        private const val KEY_SHOW_RADIAL_MENU = "show_radial_menu"
        private const val KEY_RADIAL_MENU_OPEN_MODE = "radial_menu_open_mode"
        private const val KEY_READER_CHROME_GESTURE_VERSION = "reader_chrome_gesture_version"
        private const val KEY_READER_TTS_VISIBILITY_VERSION = "reader_tts_visibility_version"
        private const val KEY_SHOW_HEADER = "show_header"
        private const val KEY_SHOW_FOOTER = "show_footer"
        private const val KEY_SHOW_FAVORITE_BUTTON = "show_favorite_button"
        private const val KEY_SCREEN_PADDING_TOP = "screen_padding_top"
        private const val KEY_SCREEN_PADDING_BOTTOM = "screen_padding_bottom"
        private const val KEY_CONTENT_WIDTH = "content_width"
        private const val KEY_REPLACE_MODE = "replace_mode"
        private const val KEY_USE_INFINITE_SCROLL = "use_infinite_scroll"
        private const val KEY_PAGE_TURN_MODE = "page_turn_mode"
        private const val KEY_PAGE_TURN_EFFECT = "page_turn_effect"
        private const val KEY_TAP_AREAS = "tap_areas"
        private const val READER_CHROME_GESTURE_VERSION = 2
        private const val READER_TTS_VISIBILITY_VERSION = 1

        private fun defaultTapAreas(): List<ReaderTapArea> = listOf(
            ReaderTapArea("left", "30%", "pagePrev"),
            ReaderTapArea("center", "40%", "sidebar"),
            ReaderTapArea("right", "30%", "pageNext"),
        )
    }
}
