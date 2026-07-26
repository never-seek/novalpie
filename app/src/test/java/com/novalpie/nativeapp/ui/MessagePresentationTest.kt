package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.MessageSettings
import com.novalpie.nativeapp.model.SiteMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessagePresentationTest {
    @Test
    fun resolvesOtherDirectMessageParticipant() {
        val message = SiteMessage(
            id = 1,
            type = 8,
            title = "dm",
            userId = 20,
            executeUserId = 10
        )

        assertEquals(20L, directMessageTargetUserId(message, currentUserId = 10))
    }

    @Test
    fun directMessageParticipantFallsBackToExecutorWhenUserIsCurrentUser() {
        val message = SiteMessage(
            id = 1,
            type = 8,
            title = "dm",
            userId = 10,
            executeUserId = 20
        )

        assertEquals(20L, directMessageTargetUserId(message, currentUserId = 10))
    }

    @Test
    fun validatesQuietHoursAndAutoReadDays() {
        assertNull(validateMessageSettings(MessageSettings()))
        assertEquals(
            "\u514d\u6253\u6270\u5f00\u59cb\u65f6\u95f4\u683c\u5f0f\u65e0\u6548",
            validateMessageSettings(MessageSettings(quietHoursStart = "25:99"))
        )
        assertEquals(
            "\u81ea\u52a8\u5df2\u8bfb\u5929\u6570\u4e0d\u80fd\u5c0f\u4e8e 0",
            validateMessageSettings(MessageSettings(autoReadAfterDays = -1))
        )
    }

    @Test
    fun validatesQuietHoursEndSeparately() {
        assertEquals(
            "\u514d\u6253\u6270\u7ed3\u675f\u65f6\u95f4\u683c\u5f0f\u65e0\u6548",
            validateMessageSettings(MessageSettings(quietHoursEnd = "7pm"))
        )
    }

    @Test
    fun mergesPagesWithoutDuplicateMessagesAndKeepsNewestVersion() {
        val merged = mergeMessagePages(
            listOf(
                SiteMessage(1, 1, "a"),
                SiteMessage(2, 1, "b")
            ),
            listOf(
                SiteMessage(2, 1, "b2"),
                SiteMessage(3, 1, "c")
            )
        )

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.id })
        assertEquals("b2", merged[1].title)
    }

    @Test
    fun messageTypeLabelsMatchCurrentWebsite() {
        assertEquals("\u5168\u90e8\u7c7b\u578b", messageTypeLabel(null))
        assertEquals("\u7528\u6237\u4e92\u52a8", messageTypeLabel(1))
        assertEquals("\u79c1\u4fe1", messageTypeLabel(8))
        assertEquals("\u4e3e\u62a5\u901a\u77e5", messageTypeLabel(10))
        assertEquals("\u672a\u77e5\u7c7b\u578b", messageTypeLabel(99))
        assertEquals((1..10).toList(), messageTypeOptions().map { it.value })
    }

    @Test
    fun selectionToggleAndVisibleSelectionAreDeterministic() {
        assertEquals(setOf(1L, 3L), toggleMessageSelection(setOf(1L, 2L, 3L), 2L))
        assertEquals(setOf(1L, 2L), toggleMessageSelection(setOf(1L), 2L))
        assertEquals(setOf(4L, 5L), selectVisibleMessages(listOf(4L, 5L), select = true))
        assertEquals(emptySet<Long>(), selectVisibleMessages(listOf(4L, 5L), select = false))
    }
}
