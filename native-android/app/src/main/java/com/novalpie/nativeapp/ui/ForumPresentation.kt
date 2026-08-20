package com.novalpie.nativeapp.ui

import androidx.compose.ui.text.AnnotatedString
import com.novalpie.nativeapp.model.ForumComment
import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.UserBadge

/** Every category owns one equal slot in the mobile forum axis. */
internal data class ForumCategorySlot(
    val category: ForumFeedCategory,
    val weight: Float = 1f,
)

internal fun forumCategorySlots(categories: List<ForumFeedCategory>): List<ForumCategorySlot> =
    categories.map(::ForumCategorySlot)

internal data class ForumCommentThread(
    val comment: ForumComment,
    val replies: List<ForumComment> = emptyList()
)

/**
 * Testable subset of the source site's Markdown/HTML model. Forum content arrives as rendered
 * HTML for some posts and raw Markdown for others, so plain-text conversion loses both emphasis
 * and routes. Keeping small segments lets the native detail page render bold text and links.
 */
internal sealed interface ForumTextSegment {
    data class Plain(val value: String) : ForumTextSegment
    data class Bold(val value: String) : ForumTextSegment
    data class Link(val label: String, val url: String) : ForumTextSegment
    /** The source uses ||...|| for content hidden by the review-feed spoiler switch. */
    data class Spoiler(val value: String) : ForumTextSegment
}

internal data class ForumRichParagraph(val segments: List<ForumTextSegment>) {
    val plainText: String
        get() = segments.joinToString(separator = "") { segment ->
            when (segment) {
                is ForumTextSegment.Plain -> segment.value
                is ForumTextSegment.Bold -> segment.value
                is ForumTextSegment.Link -> segment.label
                is ForumTextSegment.Spoiler -> segment.value
            }
        }.trim()
}

internal fun forumSpoilerIsVisible(
    hideSpoilers: Boolean,
    spoilerIndex: Int,
    revealedSpoilerIndexes: Set<Int>,
): Boolean = !hideSpoilers || spoilerIndex in revealedSpoilerIndexes

/**
 * Compose can report the exclusive end offset when a user taps the last rendered glyph. Treat the
 * preceding character as part of the hit target so a trailing spoiler remains revealable.
 */
internal fun forumStringAnnotationAtOffset(
    text: AnnotatedString,
    tag: String,
    offset: Int,
): String? {
    if (text.isEmpty()) return null
    val safeOffset = offset.coerceIn(0, text.length)
    text.getStringAnnotations(tag, safeOffset, safeOffset)
        .firstOrNull()
        ?.item
        ?.let { return it }
    if (safeOffset == 0) return null
    return text.getStringAnnotations(tag, safeOffset - 1, safeOffset - 1)
        .firstOrNull()
        ?.item
}

internal fun forumRevealSpoiler(
    hideSpoilers: Boolean,
    revealedSpoilerIndexes: Set<Int>,
    spoilerIndex: Int,
): Set<Int> = if (hideSpoilers && spoilerIndex >= 0) {
    revealedSpoilerIndexes + spoilerIndex
} else {
    revealedSpoilerIndexes
}

/** The source's review-feed toggle must not spill into unrelated discussion/detail content. */
internal fun forumFeedHideSpoilers(type: String, reviewFeedHideSpoilers: Boolean): Boolean =
    !type.trim().equals("review", ignoreCase = true) || reviewFeedHideSpoilers

/** Detail pages, replies, chapter comments, and activity previews start masked by default. */
internal fun forumContentHideSpoilers(): Boolean = true

/** A delayed down/up pair from a busy frame must not be promoted to a card navigation click. */
internal fun forumCardTapIsEligible(
    durationMillis: Long,
    distancePx: Float,
    touchSlopPx: Float,
    childConsumed: Boolean = false,
    maxDurationMillis: Long = 320L,
): Boolean = !childConsumed &&
    durationMillis >= 0L &&
    durationMillis < maxDurationMillis.coerceAtLeast(1L) &&
    distancePx <= touchSlopPx.coerceAtLeast(1f)

/** A book-review card is backed by a comment id, so its safe route is the linked book, not /forum/id. */
internal sealed interface ForumFeedDestination {
    data class Book(val bookId: Long) : ForumFeedDestination
    data class Post(val postId: Long) : ForumFeedDestination
    object None : ForumFeedDestination
}

internal fun forumFeedDestination(item: ForumFeedItem): ForumFeedDestination = when {
    item.isBookReview -> item.bookId?.takeIf { it > 0 }?.let(ForumFeedDestination::Book)
        ?: ForumFeedDestination.None
    item.id > 0 -> ForumFeedDestination.Post(item.id)
    else -> ForumFeedDestination.None
}

/** Source mobile review cards label the linked work, while regular forum cards lead with their post title. */
internal fun forumFeedTitle(item: ForumFeedItem): String =
    if (item.isBookReview) "书评 · 《${item.bookTitle}》" else item.title

/** Reviews already identify their work in the compact book line, so repeating it as a topic tag is noise. */
internal fun forumFeedTags(item: ForumFeedItem): List<String> =
    if (item.isBookReview) {
        emptyList()
    } else {
        item.tags
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(4)
    }

internal fun forumFeedCategoryLabel(category: ForumFeedCategory, reviewTotal: Int?): String =
    if (category.type == "review" && reviewTotal != null) {
        "${category.label} $reviewTotal"
    } else {
        category.label
    }

internal fun forumPostFeedItem(post: ForumPost): ForumFeedItem =
    ForumFeedItem(
        id = post.id,
        category = post.category,
        title = post.title,
        bookId = post.bookId,
        bookCoverUrl = post.bookCoverUrl,
        isBookReview = post.isBookReview,
        bookTitle = post.bookTitle ?: "站内讨论",
        authorName = post.authorName ?: "匿名用户",
        replyCount = post.replyCount ?: 0,
        likeCount = post.likeCount ?: 0,
        reactionCount = post.reactionCount ?: 0,
        awardPoints = post.awardPoints ?: 0,
        viewCount = post.viewCount ?: 0,
        lastActiveLabel = post.lastActiveLabel ?: post.createdAt ?: "刚刚",
        tags = post.tags,
        pinned = post.pinned,
        featured = post.featured,
        authorId = post.authorId,
        excerpt = post.excerpt,
        authorAvatarUrl = post.authorAvatarUrl,
        authorAvatarFrameUrl = post.authorAvatarFrameUrl,
        authorBadges = post.authorBadges,
        authorBadgeVisuals = post.authorBadgeVisuals,
        helpfulCount = post.helpfulCount ?: post.likeCount ?: 0,
        notHelpfulCount = post.notHelpfulCount ?: 0,
        funnyCount = post.funnyCount ?: post.reactionCount ?: 0,
        createdAt = post.createdAt ?: post.lastActiveLabel
    )

internal fun forumFeedBadges(item: ForumFeedItem): List<String> = buildList {
    if (item.pinned) add("置顶")
    if (item.featured) add("精华")
    item.tags.take(3).filter { it.isNotBlank() && it != item.category }.forEach(::add)
}

/**
 * Keep the source badge artwork when the API supplies it, then append only labels that do not
 * already have a visual record. The app renders the metadata natively and never executes HTML/CSS.
 */
internal fun forumAuthorBadgeVisuals(
    visuals: List<UserBadge>,
    labels: List<String>,
    maxVisible: Int = 3,
): List<UserBadge> {
    val visibleLimit = maxVisible.coerceAtLeast(1)
    val retainedVisuals = visuals.asSequence()
        .filter { it.name.isNotBlank() }
        .distinctBy { badge -> badge.id?.let { "id:$it" } ?: "name:${badge.name.trim().lowercase()}" }
        .toList()
    val visualNames = retainedVisuals.map { it.name.trim().lowercase() }.toSet()
    val fallbackVisuals = labels.asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .filter { it.lowercase() !in visualNames }
        .distinctBy { it.lowercase() }
        .map { name -> UserBadge(name = name) }
        .toList()
    return (retainedVisuals + fallbackVisuals).take(visibleLimit)
}

internal fun forumFeedMetaLine(item: ForumFeedItem): String =
    listOf(item.authorName, forumShortDateLabel(item.createdAt ?: item.lastActiveLabel))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" · ")

internal fun forumFeedMetricLabels(item: ForumFeedItem): List<String> = buildList {
    add("${item.replyCount} 条回复")
    if (item.isBookReview) {
        // The source always renders this heart counter, including a zero value.
        add("喜欢 ${item.likeCount}")
    } else {
        val helpfulCount = item.helpfulCount.takeIf { it > 0 } ?: item.likeCount
        val funnyCount = item.funnyCount.takeIf { it > 0 } ?: item.reactionCount
        if (helpfulCount > 0) add("有价值 $helpfulCount")
        if (item.notHelpfulCount > 0) add("无价值 ${item.notHelpfulCount}")
        if (funnyCount > 0) add("欢乐 $funnyCount")
        if (item.awardPoints > 0) add("奖励 ${item.awardPoints}")
    }
    add("${item.viewCount} 次浏览")
}

internal fun forumShortDateLabel(value: String?): String {
    val match = forumDatePattern.find(value.orEmpty()) ?: return value.orEmpty().ifBlank { "刚刚" }
    return "${match.groupValues[1]}/${match.groupValues[2].toInt()}/${match.groupValues[3].toInt()}"
}

internal fun forumPostDateLine(post: ForumPost): String {
    val created = forumShortDateLabel(post.createdAt ?: post.lastActiveLabel)
    val updated = post.lastActiveLabel?.let(::forumShortDateLabel)
    return if (updated.isNullOrBlank() || updated == created) {
        "发布于 $created"
    } else {
        "发布于 $created · 更新于 $updated"
    }
}

internal fun forumActionBarLabels(): List<String> =
    listOf("赞", "踩", "表情", "打赏", "网页")

internal fun forumContentLinks(paragraphs: List<String>): List<String> =
    paragraphs.flatMap { paragraph ->
        forumRichParagraphs(paragraph).flatMap { richParagraph ->
            richParagraph.segments.mapNotNull { segment ->
                (segment as? ForumTextSegment.Link)?.url
            }
        }
    }.distinct()

internal fun forumCommentLinkPreviews(comment: ForumComment): List<String> =
    forumRichParagraphs(comment.content)
        .flatMap { paragraph -> paragraph.segments }
        .mapNotNull { segment -> (segment as? ForumTextSegment.Link)?.url }
        .distinct()

internal fun forumRichParagraphs(raw: String): List<ForumRichParagraph> =
    forumMarkupParagraphs(raw)
        .map(::forumRichParagraph)
        .filter { it.segments.isNotEmpty() && it.plainText.isNotBlank() }

/**
 * Feed cards render only a few lines, so handing a whole review to Android's text layout engine
 * can stall a fling. Keep an opening spoiler marker when the cut lands inside it: the existing
 * parser then continues masking the shortened remainder instead of exposing it as plain text.
 */
internal fun forumFeedExcerptText(raw: String, maxCharacters: Int = 640): String {
    val limit = maxCharacters.coerceAtLeast(1)
    if (raw.length <= limit) return raw

    var cutoff = limit
    if (raw[cutoff - 1] == '|' && raw.getOrNull(cutoff) == '|') cutoff += 1
    return raw.take(cutoff).trimEnd() + "…"
}

/**
 * Lazy feed rows can recompose while a fling is in progress. Keep the last parsed payload in the
 * row's composition so scrolling does not repeatedly run the HTML/Markdown regex pipeline.
 */
internal class ForumRichParagraphCache(
    private val parser: (String) -> List<ForumRichParagraph> = ::forumRichParagraphs,
) {
    private var cachedRaw: String? = null
    private var cachedParagraphs: List<ForumRichParagraph>? = null

    fun get(raw: String): List<ForumRichParagraph> {
        if (cachedParagraphs == null || cachedRaw != raw) {
            cachedRaw = raw
            cachedParagraphs = parser(raw)
        }
        return cachedParagraphs.orEmpty()
    }
}

private fun forumRichParagraph(raw: String): ForumRichParagraph {
    val segments = mutableListOf<ForumTextSegment>()
    var cursor = 0
    forumInlineTokenRegex.findAll(raw).forEach { match ->
        appendForumPlainSegment(segments, raw.substring(cursor, match.range.first))
        appendForumInlineToken(segments, match.value)
        cursor = match.range.last + 1
    }
    appendForumPlainSegment(segments, raw.substring(cursor))
    return ForumRichParagraph(segments)
}

private fun appendForumInlineToken(target: MutableList<ForumTextSegment>, token: String) {
    if (token.startsWith("||")) {
        val spoilerBody = if (token.endsWith("||") && token.length >= 4) {
            token.drop(2).dropLast(2)
        } else {
            token.drop(2)
        }
        val value = spoilerBody.let(::forumInlineText).trim()
        if (value.isNotBlank()) target += ForumTextSegment.Spoiler(value)
        return
    }

    val markdownLink = forumMarkdownLinkRegex.matchEntire(token)
    if (markdownLink != null) {
        val url = forumNormalizeLink(markdownLink.groupValues[2]) ?: return
        target += ForumTextSegment.Link(
            label = forumInlineText(markdownLink.groupValues[1]).trim().ifBlank { url },
            url = url
        )
        return
    }

    if (token.startsWith("<a", ignoreCase = true)) {
        val url = forumHtmlAttribute(token, "href")?.let(::forumNormalizeLink) ?: return
        val label = forumInlineText(token.replace(forumAnchorTagRegex, "")).trim().ifBlank { url }
        target += ForumTextSegment.Link(label = label, url = url)
        return
    }

    if (token.startsWith("<strong", ignoreCase = true) || token.startsWith("<b", ignoreCase = true)) {
        val value = forumInlineText(token.replace(forumBoldTagRegex, "")).trim()
        if (value.isNotBlank()) target += ForumTextSegment.Bold(value)
        return
    }

    if (token.startsWith("**") || token.startsWith("__")) {
        val value = token.drop(2).dropLast(2).let(::forumInlineText).trim()
        if (value.isNotBlank()) target += ForumTextSegment.Bold(value)
        return
    }

    forumNormalizeLink(token)?.let { url ->
        target += ForumTextSegment.Link(label = url, url = url)
    }
}

private fun appendForumPlainSegment(target: MutableList<ForumTextSegment>, raw: String) {
    val value = forumInlineText(raw)
    if (value.isBlank()) return
    val last = target.lastOrNull()
    if (last is ForumTextSegment.Plain) {
        target[target.lastIndex] = last.copy(value = last.value + value)
    } else {
        target += ForumTextSegment.Plain(value)
    }
}

private fun forumMarkupParagraphs(raw: String): List<String> {
    val prepared = raw.trim()
        .replace(forumUnsafeBlockRegex, "")
        .replace(forumLineBreakRegex, "\n")
        .replace(forumBlockEndTagRegex, "\n\n")
        .replace(forumBlockStartTagRegex, "")

    if (prepared.isBlank()) return emptyList()
    return prepared
        .split(Regex("\\n{2,}"))
        .map { paragraph -> paragraph.lines().joinToString("\n") { line -> line.trim() }.trim() }
        .filter(String::isNotBlank)
}

private fun forumInlineText(raw: String): String =
    raw.replace(forumOtherHtmlTagRegex, "")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace(Regex("[ \\t\\u000B\\f]+"), " ")
        .replace(Regex(" *\\n *"), "\n")

private fun forumHtmlAttribute(tag: String, name: String): String? {
    val quoted = Regex(
        """\b${Regex.escape(name)}\s*=\s*(["'])(.*?)\1""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(tag)?.groupValues?.getOrNull(2)
    if (!quoted.isNullOrBlank()) return forumInlineText(quoted)
    return Regex("""\b${Regex.escape(name)}\s*=\s*([^\s>]+)""", RegexOption.IGNORE_CASE)
        .find(tag)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::forumInlineText)
}

private fun forumNormalizeLink(raw: String): String? {
    val value = forumInlineText(raw)
        .trim()
        .trimEnd('.', ',', ';', ':', '，', '。', '、', '；', '：', ')', '）')
        .takeIf(String::isNotBlank)
        ?: return null
    return when {
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("/") -> "https://novalpie.cc$value"
        else -> null
    }
}

internal fun forumCommentThreads(comments: List<ForumComment>): List<ForumCommentThread> {
    if (comments.isEmpty()) return emptyList()
    val byId = comments.associateBy { it.id }
    val repliesByRoot = linkedMapOf<Long, MutableList<ForumComment>>()
    val rootById = linkedMapOf<Long, ForumComment>()

    comments.forEach { comment ->
        val root = forumCommentRoot(comment, byId)
        if (root.id !in rootById) rootById[root.id] = root
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

private val forumDatePattern = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""")
private val forumInlineTokenRegex = Regex(
    """\|\|.*?\|\||\|\|.+$|<a\b[^>]*>.*?</a\s*>|<(?:strong|b)\b[^>]*>.*?</(?:strong|b)\s*>|\[[^\]\n]+]\(https?://[^)\s]+\)|\*\*.+?\*\*|__.+?__|https?://[^\s<>"']+""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val forumMarkdownLinkRegex = Regex("""\[([^\]]+)]\((https?://[^)\s]+)\)""", RegexOption.DOT_MATCHES_ALL)
private val forumAnchorTagRegex = Regex("""</?a\b[^>]*>""", RegexOption.IGNORE_CASE)
private val forumBoldTagRegex = Regex("""</?(?:strong|b)\b[^>]*>""", RegexOption.IGNORE_CASE)
private val forumUnsafeBlockRegex = Regex(
    """<(script|style)\b[^>]*>.*?</\1\s*>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val forumLineBreakRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
private val forumBlockEndTagRegex = Regex(
    """</(p|div|section|article|li|h[1-6]|blockquote)\s*>""",
    RegexOption.IGNORE_CASE
)
private val forumBlockStartTagRegex = Regex(
    """<(p|div|section|article|li|h[1-6]|blockquote)\b[^>]*>""",
    RegexOption.IGNORE_CASE
)
private val forumOtherHtmlTagRegex = Regex("""<(?!/?(?:a|strong|b)\b)[^>]+>""", RegexOption.IGNORE_CASE)
