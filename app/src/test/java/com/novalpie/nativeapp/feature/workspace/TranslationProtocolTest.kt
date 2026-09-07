package com.novalpie.nativeapp.feature.workspace

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TranslationProtocolTest {
    @Test fun indexedLinesPreserveBlankLinesAndImageMarkers() {
        val source = "第一行\n\n[[img:1]]\n最后一行"
        val output = decodeTranslationResponse("""{"lines":{"0":"第一行译文","1":"","2":"[[img:1]]","3":"最后译文"}}""", source)
        assertEquals("第一行译文\n\n[[img:1]]\n最后译文", output.text)
    }
    @Test fun missingLinesMalformedJsonAndChangedIllustrationsAreRejected() {
        assertTrue(runCatching { decodeTranslationResponse("""{"0":"一"}""", "一\n二") }.isFailure)
        assertTrue(runCatching { decodeTranslationResponse("""{"0":"一",broken""", "一") }.isFailure)
        assertTrue(runCatching { decodeTranslationResponse("""{"0":"[[img:2]]"}""", "[[img:1]]") }.isFailure)
    }
    @Test fun compatibleEndpointDoesNotAppendV1AfterChatCompletionsOrAcceptUserInfo() {
        assertEquals("https://fixture.test/v1/chat/completions", translationEndpoint("https://fixture.test"))
        assertEquals("https://fixture.test/v2/chat/completions", translationEndpoint("https://fixture.test/v2"))
        assertEquals("https://fixture.test/chat/completions", translationEndpoint("https://fixture.test/chat/completions"))
        assertTrue(runCatching { translationEndpoint("https://secret@fixture.test") }.isFailure)
    }
    @Test fun malformedTerminologyIsNotForwardedAsAValidChapterSubmission() {
        assertTrue(runCatching { decodeTranslationResponse("""{"lines":{"0":"译文"},"table_add":["not a term"]}""", "原文") }.isFailure)
        assertTrue(runCatching { decodeTranslationResponse("""{"lines":{"0":"译文"},"table_remove":[{"source":"bad shape"}]}""", "原文") }.isFailure)
        assertTrue(runCatching { decodeTranslationResponse("""{"lines":{"0":"译文"},"table_add":[{"source_name":"角色"}]}""", "原文") }.isFailure)
    }
}
