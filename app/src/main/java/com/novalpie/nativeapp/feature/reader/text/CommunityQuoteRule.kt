package com.novalpie.nativeapp.feature.reader.text

import com.google.re2j.Pattern

/**
 * Exact, common forum-1124 rule. RE2 does the bounded quote match; a linear, preindexed line
 * guard implements this one negative lookahead. Arbitrary assertions still stay unsupported.
 */
internal class CommunityQuoteRule private constructor(private val quote:Char) {
    fun replace(text:String,replacement:String,flags:Int):String {
        val pattern=Pattern.compile("$quote([^$quote\\n]*)$quote",flags)
        val forbidden=BooleanArray(text.length+1)
        var wordAhead=false
        for(index in text.lastIndex downTo 0) {
            if(text[index]=='\n')wordAhead=false
            if(text.startsWith("插图",index))wordAhead=true
            forbidden[index]=wordAhead
        }
        val result=StringBuilder(text.length)
        var cursor=0
        var searchFrom=0
        while(searchFrom<text.length) {
            val start=text.indexOf(quote,searchFrom)
            if(start<0)break
            if(forbidden[start+1]){searchFrom=start+1;continue}
            val end=text.indexOf(quote,start+1)
            if(end<0)break
            val newline=text.indexOf('\n',start+1)
            if(newline>=0&&newline<end){searchFrom=newline+1;continue}
            result.append(text,cursor,start)
            result.append(pattern.matcher(text.substring(start,end+1)).replaceAll(replacement))
            cursor=end+1
            searchFrom=cursor
        }
        result.append(text,cursor,text.length)
        return result.toString()
    }
    companion object {
        fun parse(source:String):CommunityQuoteRule? {
            for(quote in listOf('"','＂','\'','＇')) {
                val expected="$quote((?![^\\n]*插图)[^$quote\\n]*)$quote"
                if(source==expected)return CommunityQuoteRule(quote)
            }
            return null
        }
    }
}
