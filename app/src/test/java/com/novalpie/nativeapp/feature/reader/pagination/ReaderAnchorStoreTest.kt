package com.novalpie.nativeapp.feature.reader.pagination

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderAnchorStoreTest {
    @Test fun semanticTextOffsetsPersistWithoutMixingChaptersOrLosingLegacyProgress() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val store=ReaderAnchorStore(app)
        val first=ReaderAnchor(42,43,"43:p:9",117)
        val second=ReaderAnchor(42,44,"44:i:2",0)
        store.save(first)
        store.save(second)
        val restored=ReaderAnchorStore(app)
        assertEquals(first,restored.load(42,43))
        assertEquals(second,restored.load(42,44))
        assertNull(restored.load(41,43))
    }
}
