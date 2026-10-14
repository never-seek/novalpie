package com.novalpie.nativeapp.feature.reader.tts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.novalpie.nativeapp.feature.reader.text.readerAnnotatedTextWithWordSpacing
import com.novalpie.nativeapp.feature.reader.text.readerSpokenTextRange
import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.ui.convertChineseVariantText

/** Utterance identity, not its value: identical sentences can belong to different blocks/pages. */
internal data class ReaderSpeechFocus(val chapterId:Long,val text:String,val position:SpeechTextPosition?)

internal fun SpeechPlaybackState.readerSpeechFocus():ReaderSpeechFocus? {
    val document=chapter ?: return null
    if(status!=SpeechStatus.Speaking)return null
    val text=document.segments.getOrNull(segmentIndex) ?: return null
    return ReaderSpeechFocus(document.chapterId,text,document.positions.getOrNull(segmentIndex))
}

/** Map source offsets through display conversion/word spacing once per utterance, not per frame. */
internal fun readerSpeechDisplayRange(
    display:String,focus:ReaderSpeechFocus,chapterId:Long,blockId:String,
    variant:ChineseVariant=ChineseVariant.Original,wordSpacing:Float=0f,
):IntRange? {
    if(focus.chapterId!=chapterId)return null
    val position=focus.position
    if(position==null) {
        val query=convertChineseVariantText(focus.text,variant)
        val first=readerSpokenTextRange(display,query) ?: return null
        if(readerSpokenTextRange(display.substring(first.last+1),query)!=null)return null
        return first
    }
    if(position.blockId!=blockId)return null
    val source=position.sourceText ?: display
    if(position.textOffset !in 0..source.length)return null
    val range=readerSpokenTextRange(source.substring(position.textOffset),focus.text) ?: return null
    val from=position.textOffset+range.first;val until=position.textOffset+range.last+1
    val converted=convertChineseVariantText(source,variant)
    val start=convertChineseVariantText(source.substring(0,from),variant).length.coerceAtMost(converted.length)
    val end=convertChineseVariantText(source.substring(0,until),variant).length.coerceIn(start,converted.length)
    val marked=AnnotatedString(converted,spanStyles=listOf(AnnotatedString.Range(SpanStyle(),start,end,"speech-position")))
    val projected=readerAnnotatedTextWithWordSpacing(marked,wordSpacing)
    // A rule/doc update must not highlight an old utterance in a different, newly rendered body.
    if(projected.text!=display)return null
    val span=projected.spanStyles.single()
    return if(span.end>span.start)span.start until span.end else null
}
