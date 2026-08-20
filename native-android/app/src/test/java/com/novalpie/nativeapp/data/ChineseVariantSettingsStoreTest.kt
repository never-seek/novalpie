package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ChineseVariant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChineseVariantSettingsStoreTest {
    private lateinit var store: ChineseVariantSettingsStore

    @Before
    fun setUp() {
        store = ChineseVariantSettingsStore(ApplicationProvider.getApplicationContext())
        store.saveVariant(ChineseVariant.Original)
    }

    @Test
    fun defaultsToTheSourceOriginalMode() {
        assertEquals(ChineseVariant.Original, store.loadVariant())
    }

    @Test
    fun persistsTraditionalAndSimplifiedChoices() {
        store.saveVariant(ChineseVariant.Traditional)
        assertEquals(ChineseVariant.Traditional, store.loadVariant())

        store.saveVariant(ChineseVariant.Simplified)
        assertEquals(ChineseVariant.Simplified, store.loadVariant())
    }
}
