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
        return "免打扰开始时间格式无效"
    }
    if (!isValidOptionalClockTime(settings.quietHoursEnd)) {
        return "免打扰结束时间格式无效"
    }
    if ((settings.autoReadAfterDays ?: 0) < 0) {
        return "自动已读天数不能小于 0"
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
