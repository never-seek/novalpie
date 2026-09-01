package com.novalpie.nativeapp.model

enum class ReaderReplacementOwner {
    Personal,
    Shared,
}

enum class ReaderReplacementTarget {
    Content,
    Title,
    Both,
}

enum class ReaderReplacementRegexFlag {
    IgnoreCase,
    Multiline,
    DotMatchesAll,
}

sealed interface ReaderReplacementScope {
    data object WholeBook : ReaderReplacementScope

    data class CurrentChapter(
        val chapterOrder: Int,
    ) : ReaderReplacementScope

    data class ChapterRange(
        val startOrder: Int,
        val endOrder: Int,
    ) : ReaderReplacementScope
}

data class ReaderReplacementRule(
    val id: String,
    val novelId: Long,
    val source: String,
    val replacement: String,
    val owner: ReaderReplacementOwner = ReaderReplacementOwner.Personal,
    val sharedRuleId: String? = null,
    /** Server glossary ID for a personal rule; null means this richer local rule is device-only. */
    val websiteRuleId: Long? = null,
    val isRegex: Boolean = false,
    val regexFlags: Set<ReaderReplacementRegexFlag> = emptySet(),
    val isEnabled: Boolean = true,
    val order: Int = 0,
    val target: ReaderReplacementTarget = ReaderReplacementTarget.Content,
    val scope: ReaderReplacementScope = ReaderReplacementScope.WholeBook,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
