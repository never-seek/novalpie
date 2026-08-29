package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadSettingsStoreTest {
    @Before
    fun clearSettings() {
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences(DownloadSettingsStore.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultDownloadImageConcurrencyIsEight() {
        val store = DownloadSettingsStore(ApplicationProvider.getApplicationContext())

        assertEquals(DEFAULT_DOWNLOAD_IMAGE_CONCURRENCY, store.load().imageConcurrency)
    }

    @Test
    fun downloadImageConcurrencyPersistsAcrossStoreInstances() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        DownloadSettingsStore(context).save(DownloadSettings(imageConcurrency = 12))

        assertEquals(12, DownloadSettingsStore(context).load().imageConcurrency)
    }

    @Test
    fun persistedConcurrencyIsBoundedToSafeWorkerRange() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = DownloadSettingsStore(context)

        store.save(DownloadSettings(imageConcurrency = 0))
        assertEquals(MIN_DOWNLOAD_IMAGE_CONCURRENCY, store.load().imageConcurrency)

        store.save(DownloadSettings(imageConcurrency = Int.MAX_VALUE))
        assertEquals(MAX_DOWNLOAD_IMAGE_CONCURRENCY, store.load().imageConcurrency)
    }

    @Test
    fun customConcurrencyParserAcceptsOnlyPositiveWholeNumbers() {
        assertEquals(24, parseDownloadImageConcurrency(" 24 "))
        assertNull(parseDownloadImageConcurrency(""))
        assertNull(parseDownloadImageConcurrency("0"))
        assertNull(parseDownloadImageConcurrency("-2"))
        assertNull(parseDownloadImageConcurrency("8.5"))
    }
}
