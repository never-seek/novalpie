package com.novalpie.nativeapp.feature.reader.pagination

import com.novalpie.nativeapp.feature.reader.text.readerSpokenTextRange
import kotlin.math.abs

/** Used on document changes, never on a scroll frame. Stable text wins over a shifted list ID. */
internal fun remapReaderAnchor(anchor:ReaderAnchor,before:ChapterDocument?,after:ChapterDocument):ReaderAnchor {
    if(anchor.bookId!=after.bookId||anchor.chapterId!=after.chapterId)return ReaderAnchor(after.bookId,after.chapterId,after.blocks.firstOrNull()?.id.orEmpty())
    val oldIndex=before?.blocks?.indexOfFirst {it.id==anchor.blockId} ?: -1
    val old=before?.blocks?.getOrNull(oldIndex)
    val candidates=after.blocks.withIndex().sortedBy{abs(it.index-oldIndex.coerceAtLeast(0))}
    if(old is ChapterDocumentBlock.Image) {
        val image=candidates.firstOrNull {it.value is ChapterDocumentBlock.Image &&
            (it.value as ChapterDocumentBlock.Image).let{value->(value.originalUrl ?: value.url)==(old.originalUrl ?: old.url)}}?.value
        if(image!=null)return anchor.copy(blockId=image.id,textOffset=0)
    }
    if(old is ChapterDocumentBlock.Paragraph) {
        val paragraphs=candidates.mapNotNull {it.value as? ChapterDocumentBlock.Paragraph}.filter{it.heading==old.heading}
        paragraphs.firstOrNull {it.text.text==old.text.text}?.let{return anchor.copy(blockId=it.id,textOffset=anchor.textOffset.coerceAtMost(it.text.length))}
        val offset=anchor.textOffset.coerceIn(0,old.text.length)
        val following=old.text.text.substring(offset).take(48)
        val preceding=old.text.text.substring(0,offset).takeLast(48)
        if(following.isNotBlank())paragraphs.forEach {block->
            readerSpokenTextRange(block.text.text,following)?.let{return anchor.copy(blockId=block.id,textOffset=it.first)}
        }
        if(preceding.isNotBlank())paragraphs.forEach {block->
            readerSpokenTextRange(block.text.text,preceding)?.let{return anchor.copy(blockId=block.id,textOffset=it.last+1)}
        }
    }
    val same=after.blocks.firstOrNull {it.id==anchor.blockId}
        ?: after.blocks.getOrNull(oldIndex.coerceIn(0,after.blocks.lastIndex.coerceAtLeast(0)))
        ?: return anchor
    return anchor.copy(blockId=same.id,textOffset=if(same is ChapterDocumentBlock.Paragraph)anchor.textOffset.coerceAtMost(same.text.length)else 0)
}
