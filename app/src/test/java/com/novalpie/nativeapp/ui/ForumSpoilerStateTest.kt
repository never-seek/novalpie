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

    @Test
    fun oneSharedPreferenceUnmasksEveryForumFeedCategory() {
        assertTrue(forumFeedHideSpoilers(type = "discussion", reviewFeedHideSpoilers = true))
        assertFalse(forumFeedHideSpoilers(type = "discussion", reviewFeedHideSpoilers = false))
        assertTrue(forumFeedHideSpoilers(type = "feedback", reviewFeedHideSpoilers = true))
        assertFalse(forumFeedHideSpoilers(type = "feedback", reviewFeedHideSpoilers = false))
        assertTrue(forumFeedHideSpoilers(type = "review", reviewFeedHideSpoilers = true))
        assertFalse(forumFeedHideSpoilers(type = "review", reviewFeedHideSpoilers = false))
    }

    @Test
    fun deepFoldFallbackNeverUsesRawMarkupAsVisibleText() {
        val label = forumFoldDepthFallbackLabel("  内层内容  ")

        assertEquals("嵌套折叠层级过深，已收起：内层内容", label)
        assertFalse(label.contains("[fold", ignoreCase = true))
        assertFalse(label.contains("[/fold", ignoreCase = true))
    }
}
