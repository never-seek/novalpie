package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.BookEditInfo

data class BookEditDraft(
    val title: String = "",
    val titleTranslation: String = "",
    val authorName: String = "",
    val description: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val language: String = "zh",
    val status: String = "连载中",
    val isAdult: Boolean = false,
    val photoUrl: String = "",
    val tags: List<String> = emptyList(),
    val tagDraft: String = ""
)

internal fun bookEditDraft(info: BookEditInfo): BookEditDraft = BookEditDraft(
    title = info.title,
    titleTranslation = info.titleTranslation,
    authorName = info.authorName,
    description = info.description,
    source = info.source,
    sourceUrl = info.sourceUrl,
    language = info.language,
    status = info.status,
    isAdult = info.isAdult,
    photoUrl = info.photoUrl,
    tags = info.tags
)

internal fun validateBookEditDraft(draft: BookEditDraft): String? = when {
    draft.title.trim().isEmpty() -> "请填写中文书名"
    draft.authorName.trim().isEmpty() -> "请填写作者"
    draft.tags.any { it.trim().isEmpty() } -> "标签不能为空"
    draft.tags.distinct().size != draft.tags.size -> "标签不能重复"
    else -> null
}
