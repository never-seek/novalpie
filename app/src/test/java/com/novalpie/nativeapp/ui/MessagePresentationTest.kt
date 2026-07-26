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
            "免打扰开始时间格式无效",
            validateMessageSettings(MessageSettings(quietHoursStart = "25:99"))
        )
        assertEquals(
            "自动已读天数不能小于 0",
            validateMessageSettings(MessageSettings(autoReadAfterDays = -1))
        )
    }

    @Test
    fun validatesQuietHoursEndSeparately() {
        assertEquals(
            "免打扰结束时间格式无效",
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
        assertEquals("全部类型", messageTypeLabel(null))
        assertEquals("用户互动", messageTypeLabel(1))
        assertEquals("私信", messageTypeLabel(8))
        assertEquals("举报通知", messageTypeLabel(10))
        assertEquals("未知类型", messageTypeLabel(99))
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
