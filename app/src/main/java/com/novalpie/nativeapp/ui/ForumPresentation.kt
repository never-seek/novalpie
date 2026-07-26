package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumPost

internal data class ForumCommentThread(
    val comment: ForumComment,
    val replies: List<ForumComment> = emptyList()
)

internal fun forumPostFeedItem(post: ForumPost): ForumFeedItem =
    ForumFeedItem(
        id = post.id,
        category = post.category,
        title = post.title,
        bookTitle = post.bookTitle ?: "站内讨论",
        authorName = post.authorName ?: "匿名用户",
        replyCount = post.replyCount ?: 0,
        likeCount = post.likeCount ?: 0,
        reactionCount = post.reactionCount ?: 0,
        awardPoints = post.awardPoints ?: 0,
        viewCount = post.viewCount ?: 0,
        lastActiveLabel = post.lastActiveLabel ?: "刚刚",
        tags = post.tags.ifEmpty { listOf(post.category) },
        pinned = post.pinned,
        featured = post.featured,
        authorId = post.authorId
    )

internal fun forumFeedBadges(item: ForumFeedItem): List<String> = buildList {
    if (item.pinned) add("置顶")
    if (item.featured) add("精华")
    add(item.category)
    item.tags.take(2).forEach { tag ->
        if (tag.isNotBlank() && tag != item.category) add(tag)
    }
}

internal fun forumFeedMetaLine(item: ForumFeedItem): String =
    listOf(item.authorName, item.bookTitle, item.lastActiveLabel)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" · ")

internal fun forumFeedMetricLabels(item: ForumFeedItem): List<String> =
    listOf(
        "${item.replyCount} 条回复",
        "赞 ${item.likeCount}",
        "表情 ${item.reactionCount}",
        "打赏 ${item.awardPoints}",
        "${item.viewCount} 次浏览"
    )

internal fun forumActionBarLabels(): List<String> =
    listOf("赞", "踩", "表情", "打赏", "网页")

internal fun forumContentLinks(paragraphs: List<String>): List<String> =
    paragraphs.flatMap { paragraph ->
        urlRegex.findAll(paragraph).map { match ->
            match.value.trimEnd('.', ',', ';', ':', '，', '。', '、', '；', '：', '！', '？', ')', '）')
        }
    }.distinct()

internal fun forumCommentLinkPreviews(comment: ForumComment): List<String> =
    forumContentLinks(forumPlainParagraphs(comment.content).ifEmpty { listOf(comment.content) })

internal fun forumCommentThreads(comments: List<ForumComment>): List<ForumCommentThread> {
    if (comments.isEmpty()) return emptyList()
    val byId = comments.associateBy { it.id }
    val repliesByRoot = linkedMapOf<Long, MutableList<ForumComment>>()
    val rootById = linkedMapOf<Long, ForumComment>()

    comments.forEach { comment ->
        val root = forumCommentRoot(comment, byId)
        rootById.putIfAbsent(root.id, root)
        if (root.id != comment.id) {
            repliesByRoot.getOrPut(root.id) { mutableListOf() }.add(comment)
        }
    }

    return rootById.values.map { root ->
        ForumCommentThread(comment = root, replies = repliesByRoot[root.id].orEmpty())
    }
}

internal fun forumCommentThreadSummary(threads: List<ForumCommentThread>): String {
    val replyCount = threads.sumOf { it.replies.size }
    return "${threads.size} 条评论 · $replyCount 条回复"
}

private fun forumCommentRoot(
    comment: ForumComment,
    byId: Map<Long, ForumComment>
): ForumComment {
    val visited = mutableSetOf<Long>()
    var current = comment
    while (visited.add(current.id)) {
        val parentId = current.parentCommentId ?: return current
        current = byId[parentId] ?: return current
    }
    return comment
}

private val urlRegex = Regex("""https?://[^\s<>"']+""")

private fun forumPlainParagraphs(raw: String): List<String> {
    val text = raw
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(p|div|section|article|li|h[1-6])>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    return text.split(Regex("\n{2,}"))
        .map { paragraph -> paragraph.lines().joinToString("\n") { line -> line.trim() }.trim() }
        .filter { it.isNotBlank() }
}
