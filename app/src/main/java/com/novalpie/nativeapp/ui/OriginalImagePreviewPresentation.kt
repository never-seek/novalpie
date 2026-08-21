package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.BookCoverPhoto
import com.novalpie.nativeapp.model.NovelCard

/** Returns only a confirmed original; a card thumbnail is never a substitute for a cover preview. */
internal fun originalBookCoverPreviewUrl(
    book: NovelCard,
    photo: BookCoverPhoto?,
): String? = photo?.originalUrl.normalizedImageUrl()
    ?: book.fullCoverUrl.normalizedImageUrl()?.takeIf { full ->
        !sameImageSource(full, book.coverUrl)
    }

/**
 * Chapter image payloads occasionally carry their own original. When they do not, the book photo
 * endpoint can still resolve a repeated layered cover without replacing unrelated illustrations.
 */
internal fun originalReaderImagePreviewUrl(
    image: ReaderContentBlock.Image,
    bookPhoto: BookCoverPhoto?,
): String? = image.originalUrl.normalizedImageUrl()
    ?: bookPhoto
        ?.takeIf { sameImageSource(image.url, it.previewUrl) }
        ?.originalUrl
        .normalizedImageUrl()
    ?: image.url.normalizedImageUrl()

internal fun sameImageSource(first: String?, second: String?): Boolean {
    val normalizedFirst = first.normalizedImageUrl() ?: return false
    val normalizedSecond = second.normalizedImageUrl() ?: return false
    return normalizedFirst == normalizedSecond
}

private fun String?.normalizedImageUrl(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
