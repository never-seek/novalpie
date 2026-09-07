package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.novalpie.nativeapp.audit.ReaderTestActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic draft only. No credentials are read from the installed account and no API is called. */
class WorkspaceDraftDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    @Test fun failedApiDraftCanBeReopenedWithItsInputsInsteadOfBeingLost() {
        val state = WorkspaceState(actionMessage = "合成保存失败，草稿保留", failedApiDraft = WorkspaceApiDraft(
            name = "Beta7 synthetic", model = "fixture-model", apiKey = "synthetic-only-test", shareToServer = true))
        var saved: WorkspaceApiDraft? = null
        compose.setContent { MaterialTheme {
            WorkspaceScreen(state, {}, {}, { saved = it }, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})
        } }
        compose.onNodeWithText("继续编辑未保存的 API").performScrollTo().performClick()
        compose.onNodeWithText("Beta7 synthetic").assertExists()
        compose.onNodeWithText("fixture-model").assertExists()
        compose.onNodeWithText("保存", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertEquals("Beta7 synthetic", saved?.name)
            assertEquals("fixture-model", saved?.model)
            assertEquals("synthetic-only-test", saved?.apiKey)
        }
    }
}
