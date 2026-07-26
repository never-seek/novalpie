package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.MessageSettings
import com.novalpie.nativeapp.model.SiteMessage

internal data class MessageTypeOption(
    val value: Int,
    val label: String
)

internal fun messageTypeOptions(): List<MessageTypeOption> =
    (1..10).map { type -> MessageTypeOption(type, messageTypeLabel(type)) }

internal fun toggleMessageSelection(selected: Set<Long>, messageId: Long): Set<Long> =
    if (messageId in selected) selected - messageId else selected + messageId

internal fun selectVisibleMessages(messageIds: List<Long>, select: Boolean): Set<Long> =
    if (select) messageIds.toSet() else emptySet()

internal fun directMessageTargetUserId(
    message: SiteMessage,
    currentUserId: Long?
): Long? = listOf(message.executeUserId, message.userId)
    .firstOrNull { candidate -> candidate != null && candidate != currentUserId }

internal fun validateMessageSettings(settings: MessageSettings): String? {
    if (!isValidOptionalClockTime(settings.quietHoursStart)) {
        return "\u514d\u6253\u6270\u5f00\u59cb\u65f6\u95f4\u683c\u5f0f\u65e0\u6548"
    }
    if (!isValidOptionalClockTime(settings.quietHoursEnd)) {
        return "\u514d\u6253\u6270\u7ed3\u675f\u65f6\u95f4\u683c\u5f0f\u65e0\u6548"
    }
    if ((settings.autoReadAfterDays ?: 0) < 0) {
        return "\u81ea\u52a8\u5df2\u8bfb\u5929\u6570\u4e0d\u80fd\u5c0f\u4e8e 0"
    }
    return null
}

internal fun mergeMessagePages(
    current: List<SiteMessage>,
    next: List<SiteMessage>
): List<SiteMessage> {
    if (next.isEmpty()) return current
    return (current + next).associateBy { message -> message.id }.values.toList()
}

private fun isValidOptionalClockTime(value: String?): Boolean {
    val normalized = value?.trim().orEmpty()
    if (normalized.isEmpty()) return true
    return CLOCK_TIME_REGEX.matches(normalized)
}

private val CLOCK_TIME_REGEX = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
