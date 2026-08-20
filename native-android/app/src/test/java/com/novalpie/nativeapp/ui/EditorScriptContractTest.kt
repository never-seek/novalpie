package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorScriptContractTest {
    @Test
    fun chunkingPreservesTextAndWebsiteOptionOrder() {
        val text = "alpha line\nbeta line\ngamma line\ndelta line"

        val chunks = chunkEditorScriptText(text, targetSize = 12)

        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.size > 1)
        assertTrue(chunks.dropLast(1).all { it.endsWith("\n") })
        val options = editorScriptOptions(chunks, text.length)
        assertEquals(chunks.indices.toList(), options.map { it.chunkIndex })
        assertTrue(options.first().isFirstChunk)
        assertTrue(options.last().isLastChunk)
        assertFalse(options.first().isLastChunk)
    }

    @Test
    fun generatedProgramExposesSourceHelpersAndQuotesUserInput() {
        val program = buildEditorScriptProgram(
            script = "function processText(text, options) { return text + options.chunkIndex; }",
            text = "quote: \" and newline\nnext",
            options = EditorScriptOptions(
                mode = "full",
                chunkSize = 100,
                chunkIndex = 0,
                totalChunks = 1,
                textLength = 24,
                isFirstChunk = true,
                isLastChunk = true
            )
        )

        assertTrue(program.contains("processText"))
        assertTrue(program.contains("insertMarker"))
        assertTrue(program.contains("findMatches"))
        assertTrue(program.contains("splitByParagraphs"))
        assertTrue(program.contains("getWordCount"))
        assertTrue(program.contains("quote: \\\" and newline\\nnext"))
    }

    @Test
    fun callbackParserReturnsResultAndSurfacesScriptErrors() {
        val success = "\"{\\\"ok\\\":true,\\\"result\\\":\\\"done\\\"}\""
        val failure = "\"{\\\"ok\\\":false,\\\"error\\\":\\\"boom\\\"}\""

        assertEquals("done", parseEditorScriptCallback(success))
        val error = runCatching { parseEditorScriptCallback(failure) }.exceptionOrNull()
        assertEquals("boom", error?.message)
    }
}
