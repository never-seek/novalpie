package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.novalpie.nativeapp.model.UploadChapter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorDialogPublicationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chapterDialogCannotOpenOnAnIndexAwaitingDeletionPublication() {
        val chapters = listOf(UploadChapter("A", "A正文", 1), UploadChapter("B", "B正文", 2), UploadChapter("C", "C正文", 3))
        var state by mutableStateOf(UploadEditorState(selectedTab = EditorTab.Chapters, chapters = chapters))
        var saved: Triple<Int, String, String>? = null
        compose.setContent { MaterialTheme { Fixture(state,
            onDelete = { state = state.copy(busy = true) },
            onSave = { index, title, body -> saved = Triple(index, title, body) }) } }
        compose.onNodeWithText("A").performClick()
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("B").performTouchInput { click() }
        compose.onNodeWithText("编辑第 2 章").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(busy = false, chapters = chapters.drop(1).mapIndexed { index, c -> c.copy(chapterNumber = index + 1) }) }
        compose.onNodeWithText("B").performClick()
        compose.onNode(hasSetTextAction() and hasText("B")).performTextReplacement("B edited")
        compose.onNodeWithText("保存").performClick()
        assertEquals(Triple(0, "B edited", "B正文"), saved)
    }

    @Composable private fun Fixture(state: UploadEditorState, onDelete: (Int) -> Unit, onSave: (Int, String, String) -> Unit) {
        UploadEditorScreen(state = state, onTabSelected = {}, onOpenDocument = {}, onQueueDocuments = {},
            onRemoveQueuedDocument = {}, onImportQueuedDocuments = {}, onEncodingChange = {}, onDocumentChange = { _, _ -> },
            onMetadataChange = {}, onSplitModeChange = {}, onSplitPatternChange = {}, onSplitTargetChange = {},
            onCustomScriptChange = {}, onScriptChunkedChange = {}, onScriptChunkSizeChange = {},
            onApiEndpointChange = {}, onApiTimeoutChange = {}, onApiMarkerModeChange = {}, onBatchModeChange = {},
            onBatchTargetChange = {}, onCustomScriptResult = { _, _, _ -> }, onAiConfigSelected = {}, onGenerateAiRegex = {},
            onProcessSplit = {}, onFindChange = {}, onReplaceChange = {}, onFindRegexChange = {}, onReplaceAll = {},
            onUndo = {}, onRedo = {}, onInsertTitleMarkerAtCursor = {}, onInsertContentMarkerAtCursor = {},
            onInsertChapterAtCursor = {}, onDeleteChapterAtCursor = {}, onRenumberMarkers = {}, onValidateMarkers = {},
            onClearMarkers = {}, onUpdateChapter = onSave, onAddChapter = {}, onDeleteChapter = onDelete,
            onArchiveNameChange = {}, onSaveArchive = {}, onLoadArchive = {}, onDeleteArchive = {}, onClearArchives = {},
            onExportEpub = {}, onSendToUpload = {}, onClear = {})
    }
}
