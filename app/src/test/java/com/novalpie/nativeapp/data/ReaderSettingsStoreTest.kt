package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ReaderCustomTheme
import com.novalpie.nativeapp.model.readerCustomThemeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderSettingsStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReaderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun freshInstallUsesWebsiteReaderDefault() {
        val settings = ReaderSettingsStore(context).load()

        assertEquals(16, settings.fontSizeSp)
        assertFalse(settings.showRadialMenu)
    }

    @Test
    fun radialMenuRemainsAvailableWhenExplicitlyEnabled() {
        val store = ReaderSettingsStore(context)
        store.save(ReaderSettingsValues(showRadialMenu = true, radialMenuOpenMode = "longPress"))

        val loaded = store.load()

        assertEquals(true, loaded.showRadialMenu)
        assertEquals("longPress", loaded.radialMenuOpenMode)
    }

    @Test
    fun legacyDoubleTapDefaultMigratesToNormalTapToolbars() {
        context.getSharedPreferences(ReaderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_radial_menu", true)
            .putString("radial_menu_open_mode", "doubleTap")
            .commit()

        val loaded = ReaderSettingsStore(context).load()

        assertFalse(loaded.showRadialMenu)
        assertFalse(
            context.getSharedPreferences(ReaderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean("show_radial_menu", true),
        )
    }

    @Test
    fun legacyLongPressRadialMenuRemainsAnExplicitCustomChoice() {
        context.getSharedPreferences(ReaderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_radial_menu", true)
            .putString("radial_menu_open_mode", "longPress")
            .commit()

        assertTrue(ReaderSettingsStore(context).load().showRadialMenu)
    }

    @Test
    fun legacyHiddenTtsEntryIsRestoredOnceButLaterExplicitChoicePersists() {
        context.getSharedPreferences(ReaderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_tts", false)
            .commit()

        val store = ReaderSettingsStore(context)
        assertTrue(store.load().showTts)

        store.save(ReaderSettingsValues(showTts = false))
        assertFalse(store.load().showTts)
    }

    @Test
    fun volumeKeyPagingDefaultsOnAndReadsAnExplicitDisabledPreference() {
        val store = ReaderSettingsStore(context)

        assertTrue(store.load().volumeKeyPageTurn)

        store.save(ReaderSettingsValues(volumeKeyPageTurn = false))

        assertFalse(store.load().volumeKeyPageTurn)
    }

    @Test
    fun explicitlySavedReaderSizeSurvivesAStoreReload() {
        ReaderSettingsStore(context).saveFontSizeSp(18)

        assertEquals(18, ReaderSettingsStore(context).loadFontSizeSp())
    }

    @Test
    fun fullReaderPreferencesRoundTripAndClampToWebsiteRanges() {
        val store = ReaderSettingsStore(context)
        store.save(
            ReaderSettingsValues(
                fontSizeSp = 80,
                lineHeight = 4f,
                fontFamily = "serif",
                fontWeight = 700,
                letterSpacing = 2f,
                wordSpacing = 5f,
                theme = "green",
                emptyLine = false,
                textIndent = false,
                removeDuplicateLines = true,
                showComments = false,
                showImages = false,
                showHeader = false,
                showFooter = false,
                contentWidthDp = 100,
                useInfiniteScroll = false,
                pageTurnMode = true,
                pageTurnEffect = "none",
                tapAreas = listOf(
                    com.novalpie.nativeapp.model.ReaderTapArea("left", "25%", "pagePrev"),
                    com.novalpie.nativeapp.model.ReaderTapArea("center", "50%", "sidebar"),
                    com.novalpie.nativeapp.model.ReaderTapArea("right", "25%", "pageNext"),
                ),
            ),
        )

        val loaded = ReaderSettingsStore(context).load()
        assertEquals(48, loaded.fontSizeSp)
        assertEquals(3f, loaded.lineHeight)
        assertEquals("serif", loaded.fontFamily)
        assertEquals("green", loaded.theme)
        assertEquals(false, loaded.showImages)
        assertEquals(400, loaded.contentWidthDp)
        assertEquals(true, loaded.pageTurnMode)
        assertEquals("none", loaded.pageTurnEffect)
        assertEquals(3, loaded.tapAreas.size)
    }

    @Test
    fun customFontFamilyKeySurvivesAStoreRoundTrip() {
        val customFont = "custom-font:NotoSansCJK.otf"
        val store = ReaderSettingsStore(context)

        store.save(ReaderSettingsValues(fontFamily = customFont))

        assertEquals(customFont, store.load().fontFamily)
    }

    @Test
    fun replacementModeIsPersistedAndInvalidValuesUseWebsiteDefault() {
        val store = ReaderSettingsStore(context)
        store.save(ReaderSettingsValues(replaceMode = "genshin"))
        assertEquals("genshin", store.load().replaceMode)

        store.save(ReaderSettingsValues(replaceMode = "not-a-mode"))
        assertEquals(ReaderSettingsStore.DEFAULT_REPLACE_MODE, store.load().replaceMode)
    }

    @Test
    fun continuousScrollWinsWhenAnOldPreferenceContainsBothReadingModes() {
        val store = ReaderSettingsStore(context)
        store.save(ReaderSettingsValues(useInfiniteScroll = true, pageTurnMode = true))

        val loaded = store.load()

        assertEquals(true, loaded.useInfiniteScroll)
        assertEquals(false, loaded.pageTurnMode)
    }

    @Test
    fun legacyPageTurnPreferenceRemainsPageTurnWhenContinuousScrollKeyIsMissing() {
        context.getSharedPreferences(ReaderSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("page_turn_mode", true)
            .commit()

        val loaded = ReaderSettingsStore(context).load()

        assertEquals(false, loaded.useInfiniteScroll)
        assertEquals(true, loaded.pageTurnMode)
    }

    @Test
    fun customReaderThemesRoundTripWithColorsAndBackgroundUri() {
        val store = ReaderSettingsStore(context)
        val custom = ReaderCustomTheme(
            id = "ocean",
            name = "海边夜读",
            backgroundHex = "#102030",
            textHex = "#F0F4FF",
            sidebarBackgroundHex = "#182A40",
            sidebarTextHex = "#D9E8FF",
            accentHex = "#65A8FF",
            backgroundImageUri = "content://images/ocean.webp",
        )

        store.save(
            ReaderSettingsValues(
                theme = readerCustomThemeKey(custom.id),
                customThemes = listOf(custom),
            ),
        )

        val loaded = store.load()
        assertEquals(readerCustomThemeKey("ocean"), loaded.theme)
        assertEquals(listOf(custom), loaded.customThemes)
    }

    @Test
    fun customThemeSelectionFallsBackWhenItsDefinitionIsRemoved() {
        val store = ReaderSettingsStore(context)
        store.save(
            ReaderSettingsValues(
                theme = readerCustomThemeKey("missing"),
                customThemes = emptyList(),
            ),
        )

        assertEquals(ReaderSettingsStore.DEFAULT_THEME, store.load().theme)
    }
}
