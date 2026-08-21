package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.TerminologyEntry
import com.novalpie.nativeapp.model.TerminologyPage

internal data class TerminologyStatusPresentation(
    val label: String,
    val isWarning: Boolean = false,
)

internal fun terminologySourceLabel(entry: TerminologyEntry): String =
    entry.sourceName.trim().ifBlank { "未命名原文" }

internal fun terminologyTargetLabel(entry: TerminologyEntry): String =
    entry.targetName.trim().ifBlank { "未填写译名" }

internal fun terminologyLockPresentation(lockStatus: String?): TerminologyStatusPresentation {
    val normalized = lockStatus?.trim()?.lowercase().orEmpty()
    return when (normalized) {
        "", "0", "false", "none", "unlocked", "open" -> TerminologyStatusPresentation("未锁定")
        "1", "true", "locked", "lock" -> TerminologyStatusPresentation("已锁定", isWarning = true)
        else -> TerminologyStatusPresentation("锁定状态: ${lockStatus!!.trim()}", isWarning = true)
    }
}

internal fun terminologyActivePresentation(isActive: Boolean?): TerminologyStatusPresentation = when (isActive) {
    true -> TerminologyStatusPresentation("已启用")
    false -> TerminologyStatusPresentation("已停用", isWarning = true)
    null -> TerminologyStatusPresentation("状态未知")
}

/** The endpoint is zero-based; a total remains useful when older deployments omit totalPages. */
internal fun canLoadMoreTerminologyEntries(
    page: TerminologyPage,
    loadedCount: Int,
): Boolean = when {
    page.totalPages != null -> page.page + 1 < page.totalPages
    page.total != null -> loadedCount < page.total
    else -> page.items.size >= page.pageSize
}

internal fun terminologySummaryLabel(page: TerminologyPage, loadedCount: Int): String = when {
    page.total != null -> "已加载 $loadedCount / ${page.total} 条术语"
    else -> "已加载 $loadedCount 条术语"
}

/** A glossary query belongs to its book; never leak a prior title's text into a new route. */
internal fun terminologyKeywordForBook(state: TerminologyState, bookId: Long): String =
    state.takeIf { it.bookId == bookId }?.keyword.orEmpty()
