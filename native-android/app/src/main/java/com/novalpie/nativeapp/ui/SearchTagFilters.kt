package com.novalpie.nativeapp.ui

/** The source API uses `tags` and `blocked_tags` as two mutually exclusive tag sets. */
internal enum class SearchTagFilterMode {
    Required,
    Blocked
}

internal fun parseSearchTagInput(input: String): List<String> =
    input
        .split(',', '，', '\n')
        .map(String::trim)
        .filter(String::isNotBlank)
        .fold(emptyList()) { tags, candidate ->
            if (tags.any { it.equals(candidate, ignoreCase = true) }) tags else tags + candidate
        }

internal fun toggleSearchTagFilters(
    requiredTags: List<String>,
    blockedTags: List<String>,
    input: String,
    mode: SearchTagFilterMode
): Pair<List<String>, List<String>> {
    val candidates = parseSearchTagInput(input)
    val required = normalizeSearchTagList(requiredTags)
    val blocked = normalizeSearchTagList(blockedTags)
    if (candidates.isEmpty()) return required to blocked

    val target = if (mode == SearchTagFilterMode.Required) required else blocked
    val allAlreadySelected = candidates.all { candidate -> target.containsSearchTag(candidate) }
    if (allAlreadySelected) {
        return required.removeSearchTags(candidates) to blocked.removeSearchTags(candidates)
    }

    return when (mode) {
        SearchTagFilterMode.Required ->
            required.addSearchTags(candidates) to blocked.removeSearchTags(candidates)

        SearchTagFilterMode.Blocked ->
            required.removeSearchTags(candidates) to blocked.addSearchTags(candidates)
    }
}

internal fun removeSearchTagFilter(
    requiredTags: List<String>,
    blockedTags: List<String>,
    tagName: String
): Pair<List<String>, List<String>> =
    normalizeSearchTagList(requiredTags).removeSearchTags(listOf(tagName)) to
        normalizeSearchTagList(blockedTags).removeSearchTags(listOf(tagName))

internal fun normalizeSearchTagList(tags: List<String>): List<String> =
    tags
        .map(String::trim)
        .filter(String::isNotBlank)
        .fold(emptyList()) { values, tag ->
            if (values.any { it.equals(tag, ignoreCase = true) }) values else values + tag
        }

/** A changed rule must not leave cards from the previous rule visible on the active search page. */
internal fun searchFilterChangeShouldRefresh(
    isSearchRoute: Boolean,
    changed: Boolean,
): Boolean = isSearchRoute && changed

internal fun List<String>.containsSearchTag(tagName: String): Boolean =
    any { it.equals(tagName.trim(), ignoreCase = true) }

private fun List<String>.addSearchTags(tags: List<String>): List<String> =
    normalizeSearchTagList(this + tags)

private fun List<String>.removeSearchTags(tags: List<String>): List<String> =
    filterNot { current -> tags.any { it.equals(current, ignoreCase = true) } }
