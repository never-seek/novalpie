package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumCreatePresentationTest {
    @Test
    fun announcementCategoryIsOnlyShownToAdministrators() {
        assertEquals(
            listOf("recommend", "discussion", "feedback"),
            forumCategoryOptions(isAdmin = false).map { it.id }
        )
        assertEquals(
            listOf("announcement", "recommend", "discussion", "feedback"),
            forumCategoryOptions(isAdmin = true).map { it.id }
        )
    }

    @Test
    fun validWebsiteDraftCanBeSubmitted() {
        val result = validateForumCreateDraft(
            ForumCreateDraft(
                type = "discussion",
                title = "A useful topic",
                content = "Body",
                tags = listOf("reader"),
                pollEnabled = true,
                pollQuestion = "Choose",
                pollOptions = listOf("A", "B"),
                pollAllowMultiple = false
            ),
            isAdmin = false
        )

        assertTrue(result.canSubmit)
        assertNull(result.message)
    }

    @Test
    fun invalidPollAndNonAdminAnnouncementAreRejected() {
        val invalidPoll = validateForumCreateDraft(
            ForumCreateDraft(
                type = "discussion",
                title = "Title",
                content = "Body",
                pollEnabled = true,
                pollOptions = listOf("same", "same")
            ),
            isAdmin = false
        )
        assertFalse(invalidPoll.canSubmit)
        assertEquals("投票选项不能重复", invalidPoll.message)

        val announcement = validateForumCreateDraft(
            ForumCreateDraft(type = "announcement", title = "Title", content = "Body"),
            isAdmin = false
        )
        assertFalse(announcement.canSubmit)
        assertEquals("只有管理员可以发布公告", announcement.message)
    }
}
