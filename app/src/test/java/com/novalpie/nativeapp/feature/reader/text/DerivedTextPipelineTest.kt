package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.ReaderReplacementRule
import org.junit.Assert.*
import org.junit.Test

class DerivedTextPipelineTest {
    private fun rule(source: String, target: String, regex: Boolean = false) =
        ReaderReplacementRule("test-$source", 1, source, target, isRegex = regex)

    @Test fun textNodesChangeButHtmlAttributesAndImageUrlsAreByteForBytePreserved() {
        val raw = "<p title='Alice > Bob'>Alice <b>Alice</b> &amp; Bob</p><img src='https://host/Alice.webp' alt='Alice'>"
        val result = DerivedTextPipeline.transform(raw, listOf(rule("Alice", "艾丽丝")), 1)
        assertEquals("<p title='Alice > Bob'>艾丽丝 <b>艾丽丝</b> &amp; Bob</p><img src='https://host/Alice.webp' alt='Alice'>", result.markup)
        assertEquals(raw, result.original)
        assertFalse(result.markup.contains("host/艾丽丝"))
    }

    @Test fun markdownLinksAndEveryImageMarkerKeepTheirDestinationsAndIdentity() {
        val raw = "Alice [Alice](https://host/Alice \"Alice\") ![Alice](https://host/Alice.webp) [[img:1]] [图片1:https://host/Alice.png] https://host/Alice"
        val result = DerivedTextPipeline.transform(raw, listOf(rule("Alice", "新名字"),rule("1","2")), 1)
        assertEquals("新名字 [新名字](https://host/Alice \"Alice\") ![Alice](https://host/Alice.webp) [[img:1]] [图片1:https://host/Alice.png] https://host/Alice", result.markup)
    }

    @Test fun plainTextExportAndHtmlReaderApplyExactlyTheSameTextTransform() {
        val rules = listOf(rule("Alice", "爱丽丝"), rule("Bob", "鲍勃"))
        val plain = DerivedTextPipeline.transform("Alice sees Bob", rules, 1)
        val html = DerivedTextPipeline.transform("<p>Alice sees Bob</p>", rules, 1)
        assertEquals("<p>${plain.markup}</p>", html.markup)
        assertEquals("爱丽丝 sees 鲍勃", plain.markup)
    }

    @Test fun replacementsCannotInjectHtmlOrTurnVisibleTextIntoAnImageTag() {
        val result = DerivedTextPipeline.transform("<p>Alice</p>", listOf(rule("Alice", "<img src='https://host/tracker'>")), 1)
        assertEquals("<p>&lt;img src='https://host/tracker'&gt;</p>", result.markup)
    }

    @Test fun safeLineBreakReplacementWorksWithoutEnablingArbitraryHtml() {
        val result = DerivedTextPipeline.transform("<p>Alice\\nBob</p>", listOf(rule("\\n", "<br><br>")), 1)
        assertEquals("<p>Alice<br/><br/>Bob</p>", result.markup)
    }

    @Test fun broadRegexNeverSeesTagSyntaxOrScriptBodies() {
        val raw = "<p>Alice</p><script>const Alice=1</script><!-- Alice -->"
        val result = DerivedTextPipeline.transform(raw, listOf(rule("Alice", "B")), 1)
        assertEquals("<p>B</p><script>const Alice=1</script><!-- Alice -->", result.markup)
    }

    @Test fun escapedEntitiesParticipateInTextReplacementWithoutDoubleEscaping() {
        val result = DerivedTextPipeline.transform("<p>Tom &amp; Jerry &lt; 3</p>",listOf(rule("Tom & Jerry", "T & J")),1)
        assertEquals("<p>T &amp; J &lt; 3</p>",result.markup)
    }

    @Test fun regexFailureIsRecordedAndLeavesReadableTextIntact() {
        val bad = rule("(?=Alice)", "X", true)
        val result = DerivedTextPipeline.transform("Alice", listOf(bad), 1)
        assertEquals("Alice", result.markup)
        assertEquals(listOf(bad.id), result.invalidRuleIds)
    }

    @Test fun comparisonSymbolsAreNotMisclassifiedAsHtmlTags() {
        assertEquals("2 < 3 and five > 3",DerivedTextPipeline.transform("2 < 3 and 4 > 3",listOf(rule("4","five")),1).markup)
    }
}
