package com.novalpie.nativeapp.feature.reader.text

import androidx.compose.ui.text.AnnotatedString
import com.novalpie.nativeapp.ui.readerTextWithWordSpacing

/** Remap rich spans together with the glyphs; a display spacing preference never edits source. */
internal fun readerAnnotatedTextWithWordSpacing(value:AnnotatedString,spacing:Float):AnnotatedString {
    if(spacing==0f)return value
    val matches=Regex("(?<=\\p{L})[ ](?=\\p{L})").findAll(value.text).toList()
    if(matches.isEmpty())return value
    val replacement=readerTextWithWordSpacing("a b",spacing).removePrefix("a").removeSuffix("b")
    val offsets=IntArray(value.length+1)
    val output=StringBuilder(value.length)
    var matchIndex=0
    value.text.forEachIndexed {index,character->
        offsets[index]=output.length
        if(matches.getOrNull(matchIndex)?.range?.first==index){output.append(replacement);matchIndex++}
        else output.append(character)
    }
    offsets[value.length]=output.length
    return AnnotatedString(output.toString(),
        spanStyles=value.spanStyles.map{AnnotatedString.Range(it.item,offsets[it.start],offsets[it.end],it.tag)},
        paragraphStyles=value.paragraphStyles.map{AnnotatedString.Range(it.item,offsets[it.start],offsets[it.end],it.tag)})
}

/** Engines normalize spaces; return display offsets without re-parsing chapter markup. */
internal fun readerSpokenTextRange(display:String,spoken:String):IntRange? {
    if(spoken.isBlank())return null
    val exact=display.indexOf(spoken)
    if(exact>=0)return exact until exact+spoken.length
    fun Char.isGap()=isWhitespace()||Character.isSpaceChar(this)
    val query=spoken.filterNot{it.isGap()}
    if(query.isEmpty())return null
    val offsets=ArrayList<Int>(display.length)
    val normalized=buildString(display.length){display.forEachIndexed {index,c->if(!c.isGap()){append(c);offsets.add(index)}}}
    val start=normalized.indexOf(query)
    return if(start<0)null else offsets[start]..offsets[start+query.length-1]
}
