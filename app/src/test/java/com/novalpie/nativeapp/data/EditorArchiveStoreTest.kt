package com.novalpie.nativeapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorArchiveStoreTest {
    @Test
    fun savesLoadsListsAndDeletesFileBackedArchives() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = EditorArchiveStore(context, directoryName = "editor-test-${System.nanoTime()}")
        val archive = EditorArchive(
            id = "archive-1",
            name = "Draft",
            timestamp = 123L,
            textContent = "very long source text",
            metadata = EditorBookMetadata(title = "Book", author = "Writer"),
            fileName = "book.txt",
            chapterCount = 2,
            totalWords = 21
        )

        store.save(archive)

        assertEquals("very long source text", store.load("archive-1")?.textContent)
        assertEquals(listOf("archive-1"), store.list().map { it.id })
        store.delete("archive-1")
        assertNull(store.load("archive-1"))
    }
}
