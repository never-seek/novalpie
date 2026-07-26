package com.novalpie.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorProcessorTest {
    @Test
    fun splitsChineseAndMarkdownHeadingsInSourceOrder() {
        val text = """
            preface
            第1章 开端
            first body
            第2章 继续
            second body
        """.trimIndent()

        val chapters = EditorProcessor.splitByRegex(
            text,
            listOf("^第[\\d零一二三四五六七八九十百千万]+章.*$")
        )

        assertEquals(listOf("第1章 开端", "第2章 继续"), chapters.map { it.title })
        assertTrue(chapters.first().content.startsWith("preface"))
        assertTrue(chapters.first().content.contains("first body"))
        assertEquals(listOf(1, 2), chapters.map { it.chapterNumber })

        val markdown = EditorProcessor.splitByMarkdown("# One\nAlpha\n# Two\nBeta", level = 1)
        assertEquals(listOf("One", "Two"), markdown.map { it.title })
    }

    @Test
    fun splitsByCharacterAndParagraphTargetsWithoutDroppingText() {
        val text = (1..12).joinToString("\n\n") { "paragraph-$it content" }

        val byCharacters = EditorProcessor.splitByCharacterCount(text, targetCharacters = 55)
        val byParagraphs = EditorProcessor.splitByParagraphCount(text, targetParagraphs = 3)

        assertTrue(byCharacters.size > 1)
        assertEquals(4, byParagraphs.size)
        assertTrue(byParagraphs.first().content.contains("paragraph-1"))
        assertTrue(byParagraphs.last().content.contains("paragraph-12"))
    }

    @Test
    fun insertsAndValidatesWebsiteChapterIdentifiers() {
        val marked = EditorProcessor.toWebsiteIdentifiers(
            listOf(
                com.novalpie.nativeapp.model.UploadChapter("One", "Body 1", 1),
                com.novalpie.nativeapp.model.UploadChapter("Two", "Body 2", 2)
            )
        )

        assertTrue(marked.contains("##__T[00001]__##"))
        assertTrue(marked.contains("##__C[00002]__##"))
        assertEquals(emptyList<String>(), EditorProcessor.validateWebsiteIdentifiers(marked))
        assertTrue(EditorProcessor.validateWebsiteIdentifiers(marked.replace("##__C[00002]__##", "")).isNotEmpty())
    }

    @Test
    fun parsesWebsiteIdentifiersBackIntoOrderedChapters() {
        val marked = """
            ##__T[00001]__##
            One
            ##__C[00001]__##
            First body

            ##__T[00002]__##
            Two
            ##__C[00002]__##
            Second body
        """.trimIndent()

        val chapters = EditorProcessor.parseWebsiteIdentifiers(marked)

        assertEquals(listOf("One", "Two"), chapters.map { it.title })
        assertEquals(listOf("First body", "Second body"), chapters.map { it.content })
        assertEquals(listOf(1, 2), chapters.map { it.chapterNumber })
    }
}
