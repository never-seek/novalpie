package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookManagementSessionStateTest {
    private fun withModel(block: (NovalPieViewModel) -> Unit) {
        // Keep network loads queued: these are real synchronous state transitions, not HTTP tests.
        Dispatchers.setMain(StandardTestDispatcher())
        val store = ViewModelStore()
        try {
            val model = NovalPieViewModel(ApplicationProvider.getApplicationContext<Application>())
            store.put("root", model)
            block(model)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun signingOutImmediatelyRemovesThePreviousAccountsManagedBookDraft() = withModel { model ->
        model.loadBookEditInfo(42)
        model.updateBookEditDraft(BookEditDraft(title = "私有草稿", authorName = "作者"))
        model.updateBookTransferIdentifier("待转让目标")
        model.clearAuthToken()
        assertEquals(0L, model.bookEditState.bookId)
        assertEquals("", model.bookEditState.draft.title)
        assertEquals("", model.bookEditState.transferIdentifier)
    }

    @Test fun retryingTheSameBookPreservesEditedFieldsAndThresholds() = withModel { model ->
        model.loadBookEditInfo(42)
        model.updateBookEditDraft(BookEditDraft(title = "尚未提交的修改", authorName = "作者"))
        model.updateBookAccessPolicyDraft(BookAccessPolicyDraft(readThresholdType = "points_min", readThresholdValue = "30"))
        model.loadBookEditInfo(42)
        assertEquals("尚未提交的修改", model.bookEditState.draft.title)
        assertEquals("30", model.bookEditState.accessPolicyDraft.readThresholdValue)
    }
}
