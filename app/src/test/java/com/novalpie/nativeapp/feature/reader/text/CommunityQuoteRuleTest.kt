package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.ui.applyReaderReplacementRules
import com.novalpie.nativeapp.ui.validateReaderReplacementRule
import org.junit.Assert.*
import org.junit.Test

class CommunityQuoteRuleTest {
    @Test fun exactCommunityNegativeLookaheadIsSupportedWithoutEnablingAnUnsafeRegexEngine() {
        val rule=ReaderReplacementRule("quote",1,"\"((?![^\\n]*插图)[^\"\\n]*)\"","「\$1」",isRegex=true)
        assertTrue(validateReaderReplacementRule(rule).isValid)
        val output=applyReaderReplacementRules("\"对话\"\n\"插图1\"\n\"另一句\"",listOf(rule))
        assertEquals("「对话」\n\"插图1\"\n「另一句」",output.text)
        assertTrue(output.invalidRuleIds.isEmpty())
        assertEquals("\"不应跨\n行\"",applyReaderReplacementRules("\"不应跨\n行\"",listOf(rule)).text)
    }
    @Test fun fullWidthQuotesAndMultiplePairsUseTheSameLineGuardAsTheWebsite() {
        val source="＂((?![^\\n]*插图)[^＂\\n]*)＂"
        val rule=ReaderReplacementRule("wide",1,source,"“\$1”",isRegex=true)
        assertEquals("“你好” “再见”",applyReaderReplacementRules("＂你好＂ ＂再见＂",listOf(rule)).text)
        assertEquals("＂你好＂ 插图",applyReaderReplacementRules("＂你好＂ 插图",listOf(rule)).text)
    }
    @Test fun arbitraryLookaroundStillCannotExecuteAsABacktrackingRegex() {
        assertFalse(validateReaderReplacementRule(ReaderReplacementRule("bad",1,"(?=(a+)+b)","x",isRegex=true)).isValid)
    }
    @Test fun narrowAdapterMatchesTheOriginalRuleEvenAtSkippedAndAdjacentQuoteBoundaries() {
        for(quote in listOf('"','＂','\'','＇')) {
            val source="$quote((?![^\\n]*插图)[^$quote\\n]*)$quote"
            val rule=ReaderReplacementRule("fixture",1,source,"「\$1」",isRegex=true)
            val reference=java.util.regex.Pattern.compile(source)
            val samples=listOf("${quote}插图1$quote ${quote}后句$quote","${quote}前句$quote 插图 ${quote}后句$quote","$quote$quote$quote$quote","无引号","$quote\n$quote")
            samples.forEach {text->assertEquals(reference.matcher(text).replaceAll(rule.replacement),applyReaderReplacementRules(text,listOf(rule)).text)}
        }
    }
}
