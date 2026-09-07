package com.novalpie.nativeapp.feature.profile

import com.novalpie.nativeapp.model.UserContentActivityFeed

internal fun mergeActivityPages(previous: UserContentActivityFeed?, next: UserContentActivityFeed): UserContentActivityFeed = next.copy(
    activities = (previous?.activities.orEmpty() + next.activities).associateBy { "${it.type}:${it.id}" }.values
        .sortedWith(compareByDescending<com.novalpie.nativeapp.model.UserActivity> { it.createdAt.orEmpty().replace('T', ' ').take(19) }.thenByDescending { it.id }),
    postCount = next.postCount ?: previous?.postCount,
    forumCommentCount = next.forumCommentCount ?: previous?.forumCommentCount,
    bookReviewCount = next.bookReviewCount ?: previous?.bookReviewCount,
)

internal suspend fun reloadActivityPages(pages: Int, load: suspend (Int) -> UserContentActivityFeed): Pair<UserContentActivityFeed, Int> {
    var merged: UserContentActivityFeed? = null
    var completed = 0
    for (page in 1..pages.coerceAtLeast(1)) {
        val next = load(page)
        merged = mergeActivityPages(merged, next)
        if (next.partialFailure) break
        completed = page
        if (!next.hasMore) break
    }
    return (merged ?: UserContentActivityFeed()) to completed
}
