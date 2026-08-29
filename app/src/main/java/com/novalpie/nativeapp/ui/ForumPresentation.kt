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
    data class Italic(val value: String) : ForumTextSegment
    data class Underline(val value: String) : ForumTextSegment
    data class Strikethrough(val value: String) : ForumTextSegment
    data class InlineCode(val value: String) : ForumTextSegment
    data class CodeBlock(val value: String, val language: String? = null) : ForumTextSegment
    data class Link(val label: String, val url: String) : ForumTextSegment
    /** Source Markdown/HTML images render as their own native media block. */
    data class Image(val url: String, val alt: String = "") : ForumTextSegment
    /**
     * Source sharing syntax: [bookid:123|tags,bio]. It renders as a native book card, never raw
     * text. The optional flags deliberately mirror the website's BookCard props.
     */
    data class BookReference(
        val bookId: Long,
        val showTags: Boolean = false,
        val showBio: Boolean = false,
    ) : ForumTextSegment
    /** The source forum uses [fold:title]...[/fold] for collapsible rich-content blocks. */
    data class Fold(val title: String, val content: String) : ForumTextSegment
    /** The source uses ||...|| for content hidden by the review-feed spoiler switch. */
    data class Spoiler(val value: String) : ForumTextSegment
}

internal data class ForumRichParagraph(val segments: List<ForumTextSegment>) {
    val plainText: String
        get() = segments.joinToString(separator = "") { segment ->
            when (segment) {
                is ForumTextSegment.Plain -> segment.value
                is ForumTextSegment.Bold -> segment.value
                is ForumTextSegment.Italic -> segment.value
                is ForumTextSegment.Underline -> segment.value
                is ForumTextSegment.Strikethrough -> segment.value
                is ForumTextSegment.InlineCode -> segment.value
                is ForumTextSegment.CodeBlock -> segment.value
                is ForumTextSegment.Link -> segment.label
                is ForumTextSegment.Image -> ""
                is ForumTextSegment.BookReference -> ""
                is ForumTextSegment.Fold -> "折叠：${segment.title}"
                is ForumTextSegment.Spoiler -> segment.value
            }
        }.trim()

    val image: ForumTextSegment.Image?
        get() = segments.singleOrNull() as? ForumTextSegment.Image

    val codeBlock: ForumTextSegment.CodeBlock?
        get() = segments.singleOrNull() as? ForumTextSegment.CodeBlock

    val bookReferenceId: Long?
        get() = (segments.singleOrNull() as? ForumTextSegment.BookReference)?.bookId

    val bookReference: ForumTextSegment.BookReference?
        get() = segments.singleOrNull() as? ForumTextSegment.BookReference

    val fold: ForumTextSegment.Fold?
        get() = segments.singleOrNull() as? ForumTextSegment.Fold
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

/**
 * The native preference is shared by every surface that renders the same forum markup.  The
 * source API only uses the flag as a server-side filter for the review feed, but discussion posts
 * and detail/comment responses still contain inline `||...||` segments that must follow the same
 * local preference.  Keep the type argument for call-site/API compatibility with older tests.
 */
internal fun forumFeedHideSpoilers(type: String, reviewFeedHideSpoilers: Boolean): Boolean =
    reviewFeedHideSpoilers

/**
 * The same spoiler preference is shared by detail pages, replies, chapter comments, and activity
 * previews. The default remains masked for a fresh session, while an explicit forum preference
 * can opt every native rich-content surface into the source text.
 */
internal fun forumContentHideSpoilers(preference: Boolean = true): Boolean = preference

/** Do not print raw nested-fold syntax when a hostile/deeply nested payload reaches the renderer. */
internal fun forumFoldDepthFallbackLabel(title: String): String =
    "嵌套折叠层级过深，已收起：${title.trim().ifBlank { "折叠内容" }}"

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

/**
 * Shared navigation gate for forum feed cards and native [bookid] embeds.  Keeping the decision
 * in presentation code makes the pointer modifier and the regression suite use the same contract.
 */
internal fun forumCardNavigationAllowed(
    durationMillis: Long,
    distancePx: Float,
    touchSlopPx: Float,
    childConsumed: Boolean = false,
): Boolean = forumCardTapIsEligible(
    durationMillis = durationMillis,
    distancePx = distancePx,
    touchSlopPx = touchSlopPx,
    childConsumed = childConsumed,
)

/**
 * A navigation target inside a lazy forum list is not eligible while that list is consuming a
 * scroll. Compose's regular clickable handles ordinary tap cancellation, while this extra gate
 * covers a delayed/coalesced up event after a busy frame or a recycled row.
 */
internal fun forumNavigationTapEnabled(
    destinationAvailable: Boolean,
    isScrollInProgress: Boolean,
): Boolean = destinationAvailable && !isScrollInProgress

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
        forumLinksFromRichParagraphs(forumRichParagraphs(paragraph))
    }.distinct()

internal fun forumCommentLinkPreviews(comment: ForumComment): List<String> =
    forumLinksFromRichParagraphs(forumRichParagraphs(comment.content)).distinct()

private fun forumLinksFromRichParagraphs(paragraphs: List<ForumRichParagraph>): List<String> =
    paragraphs.flatMap { paragraph ->
        paragraph.segments.flatMap { segment ->
            when (segment) {
                is ForumTextSegment.Link -> listOf(segment.url)
                is ForumTextSegment.Fold -> forumLinksFromRichParagraphs(forumRichParagraphs(segment.content))
                else -> emptyList()
            }
        }
    }

internal fun forumRichParagraphs(raw: String): List<ForumRichParagraph> =
    forumMarkupParagraphs(raw)
        .map(::forumRichParagraph)
        .filter { paragraph ->
            paragraph.segments.isNotEmpty() &&
                (
                    paragraph.plainText.isNotBlank() ||
                        paragraph.image != null ||
                        paragraph.bookReferenceId != null ||
                        paragraph.fold != null ||
                        paragraph.codeBlock != null
                    )
        }

/**
 * Extracts only valid source sharing markers after HTML sanitisation and paragraph processing, so
 * an example inside a stripped script/style block cannot trigger a network request.
 */
internal fun forumBookReferenceIds(raw: String): List<Long> =
    forumBookReferenceIdsFromParagraphs(forumRichParagraphs(raw)).distinct()

internal fun forumBookReferenceIds(contents: Iterable<String>): List<Long> =
    contents.flatMap(::forumBookReferenceIds).distinct()

private fun forumBookReferenceIdsFromParagraphs(paragraphs: List<ForumRichParagraph>): List<Long> =
    paragraphs.flatMap { paragraph ->
        paragraph.segments.flatMap { segment ->
            when (segment) {
                is ForumTextSegment.BookReference -> listOf(segment.bookId)
                is ForumTextSegment.Fold -> forumBookReferenceIdsFromParagraphs(forumRichParagraphs(segment.content))
                else -> emptyList()
            }
        }
    }

/**
 * Feed cards render only a few lines, so handing a whole review to Android's text layout engine
 * can stall a fling. Keep an opening spoiler marker when the cut lands inside it: the existing
 * parser then continues masking the shortened remainder instead of exposing it as plain text.
 */
internal fun forumFeedExcerptText(raw: String, maxCharacters: Int = 640): String {
    val limit = maxCharacters.coerceAtLeast(1)
    // Never cut a fold between its opening and closing markers. Compact complete fold bodies to
    // their closed title form before applying the feed character budget.
    val foldSafeRaw = raw.replace(forumFoldBlockRegex) { match ->
        "[fold:${match.groupValues[1]}][/fold]"
    }.replace(forumUnclosedFoldBlockRegex) { match ->
        // The source review endpoint may already return a character-limited excerpt, so its
        // closing marker is absent. Treat the remaining body as an opaque collapsed block rather
        // than allowing [fold:...] and the hidden text to leak into the card preview.
        "[fold:${match.groupValues[1]}][/fold]"
    }
    if (foldSafeRaw.length <= limit) return foldSafeRaw

    var cutoff = limit
    if (foldSafeRaw[cutoff - 1] == '|' && foldSafeRaw.getOrNull(cutoff) == '|') cutoff += 1
    return foldSafeRaw.take(cutoff).trimEnd() + "…"
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
    forumCodeFenceRegex.matchEntire(raw.trim())?.let { match ->
        val language = match.groupValues.getOrNull(2)?.trim()?.takeIf(String::isNotBlank)
        val value = match.groupValues.getOrNull(3).orEmpty().trimEnd()
        return ForumRichParagraph(listOf(ForumTextSegment.CodeBlock(value, language)))
    }

    forumFoldBlockRegex.matchEntire(raw.trim())?.let { match ->
        val title = forumInlineText(match.groupValues[1]).trim().ifBlank { "折叠内容" }
        return ForumRichParagraph(
            listOf(
                ForumTextSegment.Fold(
                    title = title,
                    content = match.groupValues[2].trim(),
                )
            )
        )
    }

    forumBookReferenceSegment(raw.trim())?.let { reference ->
        return ForumRichParagraph(listOf(reference))
    }

    forumImageSegment(raw.trim())?.let { image ->
        return ForumRichParagraph(listOf(image))
    }

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

    forumBbCodeLinkRegex.matchEntire(token)?.let { bbCode ->
        val explicitUrl = bbCode.groupValues.getOrNull(1).orEmpty().trim()
        val labelRaw = if (explicitUrl.isNotBlank()) {
            bbCode.groupValues.getOrNull(2).orEmpty()
        } else {
            bbCode.groupValues.getOrNull(3).orEmpty()
        }
        val url = forumNormalizeLink(explicitUrl.ifBlank { labelRaw })
        if (url != null) {
            target += ForumTextSegment.Link(
                label = forumInlineText(labelRaw).trim().ifBlank { url },
                url = url,
            )
        } else {
            appendForumPlainSegment(target, token)
        }
        return
    }

    val markdownLink = forumMarkdownLinkRegex.matchEntire(token)
    if (markdownLink != null) {
        val label = forumInlineText(markdownLink.groupValues[1]).trim()
        val url = forumNormalizeLink(markdownLink.groupValues[2])
        if (url != null) {
            target += ForumTextSegment.Link(
                label = label.ifBlank { url },
                url = url,
            )
        } else if (label.isNotBlank()) {
            // The source editor inserts `(url)` as a placeholder. Keep the human label readable
            // when a user submits it unchanged instead of exposing the Markdown delimiters.
            appendForumPlainSegment(target, label)
        }
        return
    }

    if (token.startsWith("<a", ignoreCase = true)) {
        val label = forumInlineText(token.replace(forumAnchorTagRegex, "")).trim()
        val url = forumHtmlAttribute(token, "href")?.let(::forumNormalizeLink)
        if (url != null) {
            target += ForumTextSegment.Link(label = label.ifBlank { url }, url = url)
        } else if (label.isNotBlank()) {
            appendForumPlainSegment(target, label)
        }
        return
    }

    forumStyledHtmlTagRegex.matchEntire(token)?.let { styled ->
        val innerMarkup = styled.groupValues[2]
        // Source posts sometimes wrap a Markdown link in an HTML decoration tag, for example
        // `<u>[#1173](https://novalpie.cc/forum/1173)</u>`. Flattening the wrapper before the
        // Markdown pass leaves raw link syntax on screen and loses its click target. Preserve the
        // nested segments whenever they contain a link; the link remains actionable even though
        // Compose cannot apply the outer decoration to an independently clickable span.
        val nestedSegments = forumRichParagraph(innerMarkup).segments
        // The website editor freely nests Markdown inside an HTML decoration, for example
        // `<u>**全部规则**</u>`. If we flatten that wrapper with forumInlineText(), the Markdown
        // control characters leak into the native UI. Preserve every parsed nested segment (not
        // only links) so the inner text remains semantic and actionable.
        if (nestedSegments.any { it !is ForumTextSegment.Plain }) {
            target += nestedSegments
            return
        }
        val value = forumInlineText(innerMarkup).trim()
        if (value.isBlank()) return
        when (styled.groupValues[1].lowercase()) {
            "strong", "b" -> target += ForumTextSegment.Bold(value)
            "em", "i" -> target += ForumTextSegment.Italic(value)
            "u" -> target += ForumTextSegment.Underline(value)
            "s", "del", "strike" -> target += ForumTextSegment.Strikethrough(value)
            "code" -> target += ForumTextSegment.InlineCode(value)
        }
        return
    }

    if (token.startsWith("**") || token.startsWith("__")) {
        val value = token.drop(2).dropLast(2).let(::forumInlineText).trim()
        if (value.isNotBlank()) target += ForumTextSegment.Bold(value)
        return
    }

    if (token.startsWith("~~") && token.endsWith("~~")) {
        val value = token.drop(2).dropLast(2).let(::forumInlineText).trim()
        if (value.isNotBlank()) target += ForumTextSegment.Strikethrough(value)
        return
    }

    if (token.startsWith('`') && token.endsWith('`')) {
        val value = token.drop(1).dropLast(1).let(::forumInlineText).trim()
        if (value.isNotBlank()) target += ForumTextSegment.InlineCode(value)
        return
    }

    if (token.startsWith('*') && token.endsWith('*')) {
        val value = token.drop(1).dropLast(1).let(::forumInlineText).trim()
        if (value.isNotBlank()) target += ForumTextSegment.Italic(value)
        return
    }

    forumNormalizeLink(token)?.let { url ->
        target += ForumTextSegment.Link(label = url, url = url)
    }
}

private fun appendForumPlainSegment(target: MutableList<ForumTextSegment>, raw: String) {
    val value = forumInlineText(raw)
    if (value.isBlank()) {
        // A newline between two inline links is meaningful layout. Do not discard it merely
        // because it contains no visible glyph; otherwise source fold URL lists collapse into
        // one unscannable, concatenated hyperlink block.
        if (raw.contains('\n') && target.isNotEmpty()) {
            val last = target.lastOrNull()
            if (last is ForumTextSegment.Plain && !last.value.endsWith('\n')) {
                target[target.lastIndex] = last.copy(value = last.value + "\n")
            } else if (last !is ForumTextSegment.Plain || !last.value.endsWith('\n')) {
                target += ForumTextSegment.Plain("\n")
            }
        }
        return
    }
    val last = target.lastOrNull()
    if (last is ForumTextSegment.Plain) {
        target[target.lastIndex] = last.copy(value = last.value + value)
    } else {
        target += ForumTextSegment.Plain(value)
    }
}

private fun forumMarkupParagraphs(raw: String): List<String> {
    val prepared = forumNormalizeBbCodeLinks(
        forumNormalizeHtmlDetails(forumDecodeHtmlEntities(raw.trim()))
    )
        .replace(forumUnsafeBlockRegex, "")
        .replace(forumLineBreakRegex, "\n")
        .replace(forumBlockEndTagRegex, "\n\n")
        .replace(forumBlockStartTagRegex, "")

    if (prepared.isBlank()) return emptyList()
    val codeBlocks = mutableListOf<String>()
    val codeMasked = prepared.replace(forumCodeFenceRegex) { match ->
        codeBlocks += match.value
        "\n\n__NOVALPIE_CODE_${codeBlocks.lastIndex}__\n\n"
    }
    val foldBlocks = mutableListOf<String>()
    val foldMasked = codeMasked.replace(forumFoldBlockRegex) { match ->
        foldBlocks += match.value
        "\n\n__NOVALPIE_FOLD_${foldBlocks.lastIndex}__\n\n"
    }
    return foldMasked
        .split(Regex("\\n{2,}"))
        .map { paragraph -> paragraph.lines().joinToString("\n") { line -> line.trim() }.trim() }
        .filter(String::isNotBlank)
        .flatMap(::forumSeparateBlockWidgets)
        .map { paragraph ->
            forumRestoreCodePlaceholder(
                forumRestoreFoldPlaceholder(paragraph, foldBlocks),
                codeBlocks,
            )
        }
}

private fun forumRestoreCodePlaceholder(
    paragraph: String,
    codeBlocks: List<String>,
): String {
    val match = forumCodePlaceholderRegex.matchEntire(paragraph.trim()) ?: return paragraph
    return codeBlocks.getOrNull(match.groupValues[1].toIntOrNull() ?: -1) ?: paragraph
}

/**
 * Older content can contain the HTML representation produced by the website renderer. Normalize
 * that safe `details`/`summary` form to the source fold syntax before generic HTML cleanup.
 */
private fun forumNormalizeHtmlDetails(raw: String): String {
    var normalized = raw
    repeat(MAX_FORUM_PRESENTATION_FOLD_DEPTH) {
        val next = normalized.replace(forumHtmlDetailsRegex) { match ->
            val title = forumInlineText(match.groupValues[1]).trim().ifBlank { "折叠内容" }
            "[fold:$title]\n${match.groupValues[2]}\n[/fold]"
        }
        if (next == normalized) return normalized
        normalized = next
    }
    return normalized
}

private const val MAX_FORUM_PRESENTATION_FOLD_DEPTH = 8

/** Accept the common BBCode URL form used by copied forum content as an alias of Markdown links. */
private fun forumNormalizeBbCodeLinks(raw: String): String =
    raw.replace(forumBbCodeLinkRegex) { match ->
        val explicitUrl = match.groupValues.getOrNull(1).orEmpty().trim()
        val labelRaw = if (explicitUrl.isNotBlank()) {
            match.groupValues.getOrNull(2).orEmpty()
        } else {
            match.groupValues.getOrNull(3).orEmpty()
        }
        val url = forumNormalizeLink(explicitUrl.ifBlank { labelRaw })
        if (url == null) {
            match.value
        } else {
            val label = forumInlineText(labelRaw).trim().ifBlank { url }
            "[$label]($url)"
        }
    }

/**
 * Some source responses contain already-sanitized HTML as entity text (for example
 * `&lt;a href=...&gt;`). Decode only the small, presentation-safe entity set before parsing
 * tags/links; a bounded second pass handles the common double-encoded response without turning
 * arbitrary user content into executable markup.
 */
private fun forumDecodeHtmlEntities(raw: String): String {
    var decoded = raw
    repeat(2) {
        val next = decoded
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
        if (next == decoded) return decoded
        decoded = next
    }
    return decoded
}

private fun forumRestoreFoldPlaceholder(paragraph: String, foldBlocks: List<String>): String {
    val match = forumFoldPlaceholderRegex.matchEntire(paragraph.trim()) ?: return paragraph
    return foldBlocks.getOrNull(match.groupValues[1].toIntOrNull() ?: -1) ?: paragraph
}

/** Source book shares and images stay block widgets even when they occur beside ordinary text. */
private fun forumSeparateBlockWidgets(paragraph: String): List<String> {
    if (forumFoldBlockRegex.matches(paragraph.trim())) return listOf(paragraph.trim())
    if (!forumBlockWidgetRegex.containsMatchIn(paragraph)) return listOf(paragraph)

    val separated = mutableListOf<String>()
    var cursor = 0
    forumBlockWidgetRegex.findAll(paragraph).forEach { match ->
        if (forumBlockWidgetSegment(match.value) == null) return@forEach
        paragraph.substring(cursor, match.range.first)
            .trim()
            .takeIf(String::isNotBlank)
            ?.let(separated::add)
        separated += match.value
        cursor = match.range.last + 1
    }
    paragraph.substring(cursor)
        .trim()
        .takeIf(String::isNotBlank)
        ?.let(separated::add)
    return separated
}

private fun forumBlockWidgetSegment(raw: String): ForumTextSegment? =
    forumBookReferenceSegment(raw.trim()) ?: forumImageSegment(raw)

private fun forumBookReferenceSegment(raw: String): ForumTextSegment.BookReference? {
    val match = forumBookReferenceRegex.matchEntire(raw.trim()) ?: return null
    val bookId = match.groupValues[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
    val options = match.groupValues.getOrNull(2).orEmpty().lowercase()
    return ForumTextSegment.BookReference(
        bookId = bookId,
        showTags = options.contains("tag"),
        showBio = options.contains("bio") || options.contains("desc") || options.contains("description"),
    )
}

private fun forumImageSegment(raw: String): ForumTextSegment.Image? {
    val token = raw.trim()
    val markdown = forumMarkdownImageRegex.matchEntire(token)
    if (markdown != null) {
        val url = forumNormalizeImageUrl(markdown.groupValues[2]) ?: return null
        return ForumTextSegment.Image(
            url = url,
            alt = forumInlineText(markdown.groupValues[1]).trim(),
        )
    }

    if (forumHtmlImageTagRegex.matchEntire(token) == null) return null
    val url = forumHtmlAttribute(token, "data-src")
        ?: forumHtmlAttribute(token, "src")
        ?: return null
    return forumNormalizeImageUrl(url)?.let { normalizedUrl ->
        ForumTextSegment.Image(
            url = normalizedUrl,
            alt = forumHtmlAttribute(token, "alt").orEmpty().trim(),
        )
    }
}

private fun forumInlineText(raw: String): String =
    raw.replace(forumOtherHtmlTagRegex, "")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        // Markdown headings are source presentation markers, not literal forum text.
        .replace(Regex("(?m)^[ \\t]{0,3}#{1,6}[ \\t]+"), "")
        // Keep quoted/list content readable without exposing its control characters.
        .replace(Regex("(?m)^[ \\t]{0,3}>[ \\t]?"), "")
        .replace(Regex("(?m)^[ \\t]{0,3}[-*+][ \\t]+"), "• ")
        .replace(Regex("(?m)^[ \\t]{0,3}\\d+[.)][ \\t]+"), "")
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
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://novalpie.cc$value"
        // Markdown permits relative destinations. The editor's default `url` placeholder is one
        // such destination; resolving it to the source origin prevents `[label](url)` from leaking
        // raw brackets while retaining the website's normal navigation semantics.
        ':' !in value && !value.startsWith("#") && !value.any(Char::isWhitespace) ->
            "https://novalpie.cc/${value.trimStart('/')}"
        else -> null
    }
}

/** Images are fetched only from HTTP(S) or a source-relative site path; executable/data URLs stay text. */
private fun forumNormalizeImageUrl(raw: String): String? {
    val value = forumInlineText(raw).trim().takeIf(String::isNotBlank) ?: return null
    return when {
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://novalpie.cc$value"
        ':' !in value && !value.startsWith('#') -> "https://novalpie.cc/${value.trimStart('/')}"
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

/**
 * Forum detail keeps the composer immediately after the back link, detail header, and section
 * title. When a user replies to a comment several screens down, this is the item that must be
 * brought into view so the target and send action are not silently off-screen.
 */
internal const val FORUM_POST_DETAIL_COMPOSER_ITEM_INDEX = 3

internal fun forumPostDetailComposerScrollIndex(replyingToCommentId: Long?): Int? =
    replyingToCommentId?.takeIf { it > 0L }?.let { FORUM_POST_DETAIL_COMPOSER_ITEM_INDEX }

/**
 * Resolve the thread root used by the source forum composer and nested interaction endpoints.
 *
 * The visible reply can be several levels below the root, but the website always sends the root
 * comment id in `comment_id` and keeps the directly selected author's name in `reply_to_name`.
 * When a paged response omits an ancestor, the known parent id is the safest root fallback.
 */
internal fun forumCommentThreadRootId(
    comment: ForumComment,
    availableComments: List<ForumComment> = emptyList(),
): Long {
    val byId = availableComments.associateBy(ForumComment::id)
    val visited = mutableSetOf<Long>()
    var current = comment
    while (visited.add(current.id)) {
        val parentId = current.parentCommentId ?: return current.id
        val parent = byId[parentId] ?: return parentId
        current = parent
    }
    return current.id
}

internal fun forumReplySubmissionCommentId(
    comment: ForumComment,
    availableComments: List<ForumComment> = emptyList(),
): Long = forumCommentThreadRootId(comment, availableComments)

/**
 * Keep the reply body independent from the direct reply target.
 *
 * The website shows `回复 @name` as composer chrome and sends the name in `reply_to_name`; it does
 * not inject an `@name` token into the content body. Clear a legacy auto-prefix left by an older
 * native build, but never create one for a newly selected target.
 */
internal fun replyComposerDraftForTarget(
    currentDraft: String,
    previousTargetName: String?,
    nextTargetName: String?,
): String {
    if (currentDraft.isBlank()) return ""
    val previousPrefix = previousTargetName?.trim()?.takeIf(String::isNotBlank)?.let { "@$it " }
    return if (previousPrefix != null && currentDraft == previousPrefix) "" else currentDraft
}

/** The exact insertions exposed by the source Markdown editor, kept independent from Compose. */
internal enum class ForumCommentMarkupAction(val label: String) {
    Bold("粗体"),
    Italic("斜体"),
    Heading("标题"),
    Quote("引用"),
    Code("代码"),
    Link("链接"),
    List("列表"),
    Underline("下划线"),
    Strikethrough("删除线"),
    Spoiler("黑幕"),
    Fold("折叠"),
}

internal data class ForumCommentMarkupEdit(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)

/**
 * Insert or wrap the current selection using the same source syntax as the website editor. The
 * returned selection intentionally stays on the editable inner text, so repeated toolbar taps
 * do not strand a cursor outside an inserted marker pair.
 */
internal fun forumCommentMarkupEdit(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    action: ForumCommentMarkupAction,
): ForumCommentMarkupEdit {
    val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val end = maxOf(selectionStart, selectionEnd).coerceIn(start, text.length)
    val selected = text.substring(start, end)

    if (action == ForumCommentMarkupAction.Fold) {
        val title = selected.ifBlank { "点击展开" }
        val body = selected.ifBlank { "这里是可以折叠的内容..." }
        val prefix = "[fold:$title]\n"
        val replacement = "$prefix$body\n[/fold]"
        return ForumCommentMarkupEdit(
            text = text.replaceRange(start, end, replacement),
            selectionStart = start + prefix.length,
            selectionEnd = start + prefix.length + body.length,
        )
    }

    val template = when (action) {
        ForumCommentMarkupAction.Bold -> Triple("**", "**", "粗体文字")
        ForumCommentMarkupAction.Italic -> Triple("*", "*", "斜体文字")
        ForumCommentMarkupAction.Heading -> Triple("## ", "", "标题")
        ForumCommentMarkupAction.Quote -> Triple("> ", "", "引用文字")
        ForumCommentMarkupAction.Code -> Triple("`", "`", "代码")
        ForumCommentMarkupAction.Link -> Triple("[", "](url)", "链接文字")
        ForumCommentMarkupAction.List -> Triple("- ", "", "列表项")
        ForumCommentMarkupAction.Underline -> Triple("<u>", "</u>", "下划线文字")
        ForumCommentMarkupAction.Strikethrough -> Triple("~~", "~~", "删除线文字")
        ForumCommentMarkupAction.Spoiler -> Triple("||", "||", "黑幕文字")
        ForumCommentMarkupAction.Fold -> error("fold handled above")
    }
    val body = selected.ifBlank { template.third }
    val replacement = template.first + body + template.second
    return ForumCommentMarkupEdit(
        text = text.replaceRange(start, end, replacement),
        selectionStart = start + template.first.length,
        selectionEnd = start + template.first.length + body.length,
    )
}

internal data class ForumCommentActionTarget(
    val parentCommentId: Long,
    val replyId: Long? = null,
)

/** Resolve the source immediate-parent/reply pair used by forum like, reaction, and award endpoints. */
internal fun forumCommentActionTarget(
    comment: ForumComment,
    availableComments: List<ForumComment> = emptyList(),
): ForumCommentActionTarget {
    val rootId = forumCommentThreadRootId(comment, availableComments)
    if (rootId == comment.id) return ForumCommentActionTarget(parentCommentId = rootId)
    return ForumCommentActionTarget(
        parentCommentId = rootId,
        replyId = comment.id,
    )
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
private val forumBookReferenceRegex = Regex(
    """\[bookid\s*:\s*([1-9]\d*)\s*(?:\|\s*([^\]\r\n]+?))?\s*\]""",
    RegexOption.IGNORE_CASE,
)
private val forumFoldBlockRegex = Regex(
    """\[fold\s*:\s*([^\]\r\n]+?)\]\s*([\s\S]*?)\s*\[/fold\s*\]""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val forumUnclosedFoldBlockRegex = Regex(
    """\[fold\s*:\s*([^\]\r\n]+?)\](?:(?!\[/fold\s*\])[\s\S])*\z""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val forumHtmlDetailsRegex = Regex(
    """<details\b[^>]*>\s*<summary\b[^>]*>([\s\S]*?)</summary\s*>([\s\S]*?)</details\s*>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val forumCodeFenceRegex = Regex(
    """(?m)^[ \t]{0,3}(`{3,}|~{3,})[ \t]*([^\r\n]*)\r?\n([\s\S]*?)\r?\n^[ \t]{0,3}\1[ \t]*(?=\r?$)""",
    setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
)
private val forumCodePlaceholderRegex = Regex("""__NOVALPIE_CODE_(\d+)__""")
private val forumFoldPlaceholderRegex = Regex("""__NOVALPIE_FOLD_(\d+)__""")
private val forumMarkdownImageRegex = Regex(
    """!\[([^\]]*)]\(\s*([^\s)]+)(?:\s+["'][^"']*["'])?\s*\)""",
    RegexOption.DOT_MATCHES_ALL,
)
private val forumHtmlImageTagRegex = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val forumBlockWidgetRegex = Regex(
    """(?:${forumBookReferenceRegex.pattern})|(?:${forumMarkdownImageRegex.pattern})|(?:${forumHtmlImageTagRegex.pattern})""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val forumInlineTokenRegex = Regex(
    """\|\|.*?\|\||\|\|.+$|<a\b[^>]*>.*?</a\s*>|<(?:strong|b|em|i|u|s|del|strike|code)\b[^>]*>.*?</(?:strong|b|em|i|u|s|del|strike|code)\s*>|\[[^\]\n]+]\([^)\s]+\)|\*\*.+?\*\*|__.+?__|(?<!\*)\*(?!\*)[^\n*]+?(?<!\*)\*(?!\*)|~~[^\n~]+?~~|`[^\n`]+?`|https?://[^\s<>"']+""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val forumMarkdownLinkRegex = Regex("""\[([^\]]+)]\(([^)\s]+)\)""", RegexOption.DOT_MATCHES_ALL)
private val forumBbCodeLinkRegex = Regex(
    """\[url\s*=\s*([^\]\r\n]+)]([\s\S]*?)\[/url\s*]|\[url\]([\s\S]*?)\[/url\s*]""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val forumAnchorTagRegex = Regex("""</?a\b[^>]*>""", RegexOption.IGNORE_CASE)
private val forumStyledHtmlTagRegex = Regex(
    """<(strong|b|em|i|u|s|del|strike|code)\b[^>]*>(.*?)</\1\s*>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
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
