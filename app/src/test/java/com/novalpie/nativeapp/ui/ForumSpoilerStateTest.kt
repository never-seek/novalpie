package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForumSpoilerStateTest {
    @Test
    fun spoilerPreferenceCanChangeWhileViewingDiscussionContent() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = NovalPieViewModel(application)

        assertEquals("discussion", viewModel.forumState.selectedType)
        assertTrue(viewModel.forumState.hideSpoilers)

        viewModel.updateForumHideSpoilers(false)

        assertFalse(viewModel.forumState.hideSpoilers)
    }
}
