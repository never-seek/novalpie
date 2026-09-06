package com.novalpie.nativeapp.feature.reader.tts

import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.ui.ReaderContentBlock
import com.novalpie.nativeapp.ui.readerBlocksForContent
import com.novalpie.nativeapp.ui.readerBlocksForDisplay
import com.novalpie.nativeapp.ui.readerTtsSegments
import com.novalpie.nativeapp.feature.reader.text.readerSpokenTextRange

internal data class SpeechTextPosition(val blockId:String,val itemIndexWithinChapter:Int,val textOffset:Int)

/** Shared by initial foreground playback and background continuation, without ever voicing images. */
internal fun buildSpeechChapter(
    bookId:Long,chapterId:Long,bookTitle:String,content:ReaderContent,nextChapterId:Long?,chapterNumber:Int?,chapterCount:Int?,revision:Long,
    showImages:Boolean=true,removeDuplicateLines:Boolean=false,preparedBlocks:List<ReaderContentBlock>?=null,
):SpeechChapter {
    val paragraphs=preparedBlocks ?: readerBlocksForDisplay(readerBlocksForContent(content),removeDuplicateLines)
    val segments=mutableListOf<String>()
    val positions=mutableListOf<SpeechTextPosition>()
    var itemIndex=if(content.title.isNullOrBlank())0 else 1
    paragraphs.forEachIndexed {blockIndex,block->when(block) {
        is ReaderContentBlock.Image->if(showImages)itemIndex++
        is ReaderContentBlock.Text->{
            var searchFrom=0
            readerTtsSegments(listOf(block.value)).forEach {segment->
                val range=readerSpokenTextRange(block.value.substring(searchFrom),segment)
                val start=range?.first?.plus(searchFrom) ?: searchFrom
                segments+=segment
                positions+=SpeechTextPosition("$chapterId:p:$blockIndex",itemIndex,start)
                searchFrom=range?.last?.plus(searchFrom)?.plus(1) ?: start
            }
            itemIndex++
        }
    }}
    return SpeechChapter(bookId,chapterId,bookTitle,content.title.orEmpty(),segments,nextChapterId,revision,positions,chapterNumber,chapterCount)
}
