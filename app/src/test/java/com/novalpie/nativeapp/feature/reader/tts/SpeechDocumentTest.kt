package com.novalpie.nativeapp.feature.reader.tts

import com.novalpie.nativeapp.model.ReaderContent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeechDocumentTest {
    @Test fun speechSegmentsKeepParagraphAnchorsAndNeverCountAnImageAsSpeech() {
        val text=ReaderContent("本章标题","<p>第一段。</p><img src='https://image.test/one.webp'><p>第二段。</p>","test")
        val chapter=buildSpeechChapter(1,2,"书",text,3,4,5,6,showImages=true)
        assertEquals(listOf("第一段。","第二段。"),chapter.segments)
        assertEquals(listOf(1,3),chapter.positions.map{it.itemIndexWithinChapter})
        assertEquals(listOf("2:p:0","2:p:2"),chapter.positions.map{it.blockId})
        assertEquals(4,chapter.chapterNumber)
        assertEquals(5,chapter.chapterCount)
        val hidden=buildSpeechChapter(1,2,"书",text,3,4,5,6,showImages=false)
        assertEquals(listOf(1,2),hidden.positions.map{it.itemIndexWithinChapter})
        assertEquals(chapter.segments,hidden.segments)
    }
}
