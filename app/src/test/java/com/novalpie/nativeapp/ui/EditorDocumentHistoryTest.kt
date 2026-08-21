package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDocumentHistoryTest {
    @Test
    fun undoAndRedoRestoreTheDocumentAndClearRedoAfterANewEdit() {
        val history = EditorDocumentHistory()
        val initial = snapshot("alpha", 5)
        val withMarker = snapshot("alpha\n##__T[00001]__##", 5)
        val replacement = snapshot("beta", 4)

        history.record(initial, withMarker)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(initial, history.undo(withMarker))
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertEquals(withMarker, history.redo(initial))

        assertEquals(initial, history.undo(withMarker))
        history.record(initial, replacement)
        assertFalse(history.canRedo)
        assertNull(history.redo(replacement))
        assertEquals(initial, history.undo(replacement))
    }

    @Test
    fun oversizedSnapshotsDoNotRetainAnotherFullEditorCopy() {
        val history = EditorDocumentHistory(maxEntries = 8, maxCharacters = 10)
        val small = snapshot("small", 5)
        val large = snapshot("this document is intentionally too large", 39)

        history.record(small, large)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    private fun snapshot(text: String, cursor: Int) = EditorDocumentSnapshot(
        text = text,
        cursorPosition = cursor,
        chapters = emptyList(),
        markerValidationErrors = emptyList()
    )
}
