package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppThemeSettingsStoreTest {
    private lateinit var store: AppThemeSettingsStore

    @Before
    fun setUp() {
        store = AppThemeSettingsStore(ApplicationProvider.getApplicationContext())
        store.saveMode(AppThemeMode.System)
    }

    @Test
    fun defaultsToSystemUntilTheUserChoosesASourceStyleOverride() {
        assertEquals(AppThemeMode.System, store.loadMode())
    }

    @Test
    fun persistsLightAndDarkOverrides() {
        store.saveMode(AppThemeMode.Dark)
        assertEquals(AppThemeMode.Dark, store.loadMode())

        store.saveMode(AppThemeMode.Light)
        assertEquals(AppThemeMode.Light, store.loadMode())
    }
}
