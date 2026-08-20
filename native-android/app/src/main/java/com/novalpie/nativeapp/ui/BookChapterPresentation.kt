package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ManagedBookAccessPolicy

data class ManagedChapterDraft(
    val chapterId: Long? = null,
    val insertAt: Int = 1,
    val title: String = "",
    val content: String = ""
)

internal fun validateManagedChapterDraft(draft: ManagedChapterDraft): String? = when {
    draft.chapterId == null && draft.insertAt < 1 -> "插入位置必须大于 0"
    draft.title.trim().isEmpty() -> "章节标题不能为空"
    draft.content.trim().isEmpty() -> "章节内容不能为空"
    else -> null
}

data class BookAccessPolicyDraft(
    val allowDownload: Boolean = true,
    val downloadThresholdType: String = "none",
    val downloadThresholdValue: String = "0",
    val readThresholdType: String = "none",
    val readThresholdValue: String = "0"
)

internal fun validateBookAccessPolicyDraft(draft: BookAccessPolicyDraft): String? {
    validateThresholdDraft(
        type = if (draft.allowDownload) draft.downloadThresholdType else "none",
        value = if (draft.allowDownload) draft.downloadThresholdValue else "0",
        label = "下载门槛"
    )?.let { return it }
    validateThresholdDraft(draft.readThresholdType, draft.readThresholdValue, "阅读门槛")?.let { return it }
    return null
}

internal fun bookAccessPolicyFromDraft(draft: BookAccessPolicyDraft): ManagedBookAccessPolicy {
    val downloadType = if (draft.allowDownload) draft.downloadThresholdType.trim().ifBlank { "none" } else "none"
    val downloadValue = if (downloadType == "none") 0 else draft.downloadThresholdValue.toIntOrNull() ?: 0
    val readType = draft.readThresholdType.trim().ifBlank { "none" }
    val readValue = if (readType == "none") 0 else draft.readThresholdValue.toIntOrNull() ?: 0
    return ManagedBookAccessPolicy(
        allowDownload = draft.allowDownload,
        downloadThresholdType = downloadType,
        downloadThresholdValue = downloadValue,
        readThresholdType = readType,
        readThresholdValue = readValue
    )
}

internal fun chapterIllustrationPlaceholder(index: Int): String = "[[img:${index.coerceAtLeast(1)}]]"

private fun validateThresholdDraft(type: String, value: String, label: String): String? {
    val normalizedType = type.trim().ifBlank { "none" }
    if (normalizedType !in setOf("none", "points_min", "points_pay")) return "$label 类型无效"
    if (normalizedType == "none") return null
    val parsed = value.trim().toIntOrNull() ?: return "$label 必须是数字"
    if (parsed <= 0) return "$label 必须大于 0"
    val max = if (normalizedType == "points_pay") 50 else 100
    return if (parsed > max) "$label 不能超过 $max" else null
}
