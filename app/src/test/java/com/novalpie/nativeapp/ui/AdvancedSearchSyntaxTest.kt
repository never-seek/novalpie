package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedSearchSyntaxTest {
    @Test
    fun titleScopeAndExcludedTermMatchTheObservedWebsiteRequest() {
        val request = resolveSearchRequest(
            keyword = "@title:魔法 学院 NOT 续作",
            options = SearchOptions(advancedSyntaxEnabled = true)
        )

        assertEquals("魔法 学院", request.keyword)
        assertEquals("title", request.scope)
        assertEquals(listOf("续作"), request.blockedTerms)
        assertTrue(request.errors.isEmpty())
    }

    @Test
    fun authorScopeAndBlockedTagAreResolvedSeparately() {
        val request = resolveSearchRequest(
            keyword = "in:author 叶轻灵 NOT tag:虐心",
            options = SearchOptions(advancedSyntaxEnabled = true)
        )

        assertEquals("叶轻灵", request.keyword)
        assertEquals("author", request.scope)
        assertEquals(listOf("虐心"), request.blockedTags)
        assertTrue(request.errors.isEmpty())
    }

    @Test
    fun wordPlatformTypeStatusAndMatchSyntaxOverrideTheBasicOptions() {
        val request = resolveSearchRequest(
            keyword = "word:10w..50w platform:novelPia type:玄幻 status:连载 match:loose",
            options = SearchOptions(
                advancedSyntaxEnabled = true,
                wordCountRange = "1000000..",
                matchType = "fuzzy_strict",
                source = "upload"
            )
        )

        assertEquals("", request.keyword)
        assertEquals(100_000L, request.minWordCount)
        assertEquals(500_000L, request.maxWordCount)
        assertEquals("novelPia", request.platform)
        assertEquals("", request.source)
        assertEquals("玄幻", request.type)
        assertEquals("连载", request.status)
        assertEquals("fuzzy_loose", request.matchType)
        assertTrue(request.errors.isEmpty())
    }

    @Test
    fun tagAlternativesAndExplicitOperatorsDoNotLeakIntoKeywords() {
        val pipeRequest = resolveSearchRequest(
            keyword = "tag:恋爱|校园",
            options = SearchOptions(advancedSyntaxEnabled = true)
        )
        val explicitOperatorRequest = resolveSearchRequest(
            keyword = "tag:恋爱 AND tag:校园 OR tag:轻小说",
            options = SearchOptions(advancedSyntaxEnabled = true)
        )
        val normalTermRequest = resolveSearchRequest(
            keyword = "魔法 OR 学院",
            options = SearchOptions(advancedSyntaxEnabled = true)
        )

        assertEquals(listOf("恋爱", "校园"), pipeRequest.tagsAny)
        assertEquals("", pipeRequest.keyword)
        assertEquals(listOf("恋爱", "校园", "轻小说"), explicitOperatorRequest.requiredTags)
        assertEquals("", explicitOperatorRequest.keyword)
        assertEquals("魔法 学院", normalTermRequest.keyword)
    }

    @Test
    fun parenthesizedTagExpressionUsesTheDedicatedSourceParameter() {
        val request = resolveSearchRequest(
            keyword = "tag:(恋爱 AND 校园) OR 轻小说 word:10w..50w",
            options = SearchOptions(advancedSyntaxEnabled = true)
        )

        assertEquals("", request.keyword)
        assertEquals("(恋爱 AND 校园) OR 轻小说", request.tagsExpression)
        assertEquals(100_000L, request.minWordCount)
        assertEquals(500_000L, request.maxWordCount)
        assertTrue(request.errors.isEmpty())
    }

    @Test
    fun invalidSyntaxIsReportedBeforeTheNetworkRequest() {
        val trailingNot = parseAdvancedSearchSyntax("奇幻 NOT")
        val invertedRange = parseAdvancedSearchSyntax("word:50w..10w")

        assertFalse(trailingNot.errors.isEmpty())
        assertTrue(invertedRange.errors.any { it.contains("下限") })
    }
}
