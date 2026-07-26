package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.UploadChapter

object EditorProcessor {
    private val titleIdentifier = Regex("^##__T\\[(\\d{5})]__##$", RegexOption.MULTILINE)
    private val contentIdentifier = Regex("^##__C\\[(\\d{5})]__##$", RegexOption.MULTILINE)

    fun splitByRegex(text: String, patterns: List<String>): List<UploadChapter> {
        val matches = patterns
            .filter(String::isNotBlank)
            .flatMap { pattern -> Regex(pattern, setOf(RegexOption.MULTILINE)).findAll(text).toList() }
            .distinctBy { it.range.first }
            .sortedBy { it.range.first }
        return chaptersFromMatches(text, matches) { match -> match.value.trim() }
    }

    fun splitByMarkdown(text: String, level: Int): List<UploadChapter> {
        require(level in 1..6) { "Markdown 标题层级必须介于 1 到 6" }
        val matches = Regex("^#{${level}}[ \\t]+(.+)$", setOf(RegexOption.MULTILINE))
            .findAll(text)
            .toList()
        return chaptersFromMatches(text, matches) { match -> match.groupValues[1].trim() }
    }

    fun splitByKeywordNumber(text: String, keywords: List<String>): List<UploadChapter> {
        val alternatives = keywords.map(String::trim).filter(String::isNotEmpty).distinct().joinToString("|") { Regex.escape(it) }
        if (alternatives.isBlank()) return emptyList()
        val matches = Regex("^.*(?:$alternatives).*\\d+.*$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
            .findAll(text)
            .toList()
        return chaptersFromMatches(text, matches) { match -> match.value.trim() }
    }

    fun splitByCharacterCount(text: String, targetCharacters: Int): List<UploadChapter> {
        require(targetCharacters > 0) { "目标字数必须大于 0" }
        val paragraphs = text.split(Regex("\\n\\s*\\n")).map(String::trim).filter(String::isNotEmpty)
        if (paragraphs.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        var currentLength = 0
        paragraphs.forEach { paragraph ->
            val additional = paragraph.length + if (current.isEmpty()) 0 else 2
            if (current.isNotEmpty() && currentLength + additional > targetCharacters) {
                groups += current
                current = mutableListOf()
                currentLength = 0
            }
            current += paragraph
            currentLength += paragraph.length + if (current.size == 1) 0 else 2
        }
        if (current.isNotEmpty()) groups += current
        return groups.mapIndexed { index, group ->
            UploadChapter("第 ${index + 1} 章", group.joinToString("\n\n"), index + 1)
        }
    }

    fun splitByParagraphCount(text: String, targetParagraphs: Int): List<UploadChapter> {
        require(targetParagraphs > 0) { "目标段落数必须大于 0" }
        return text
            .split(Regex("\\n\\s*\\n"))
            .map(String::trim)
            .filter(String::isNotEmpty)
            .chunked(targetParagraphs)
            .mapIndexed { index, paragraphs ->
                UploadChapter("第 ${index + 1} 章", paragraphs.joinToString("\n\n"), index + 1)
            }
    }

    fun toWebsiteIdentifiers(chapters: List<UploadChapter>): String = chapters.joinToString("\n") { chapter ->
        val number = chapter.chapterNumber.coerceAtLeast(1).toString().padStart(5, '0')
        "##__T[$number]__##\n${chapter.title}\n##__C[$number]__##\n${chapter.content}"
    }

    fun validateWebsiteIdentifiers(text: String): List<String> {
        val titles = titleIdentifier.findAll(text).map { it.groupValues[1] }.toList()
        val contents = contentIdentifier.findAll(text).map { it.groupValues[1] }.toList()
        val errors = mutableListOf<String>()
        (titles - contents.toSet()).forEach { errors += "章节 $it 缺少内容标识符" }
        (contents - titles.toSet()).forEach { errors += "章节 $it 缺少标题标识符" }
        if (titles.size != titles.distinct().size) errors += "存在重复的标题标识符"
        if (contents.size != contents.distinct().size) errors += "存在重复的内容标识符"
        if (titles.toSet() == contents.toSet() && titles != contents) errors += "标题与内容标识符顺序不一致"
        return errors
    }

    fun parseWebsiteIdentifiers(text: String): List<UploadChapter> {
        val pattern = Regex(
            "^##__T\\[(\\d{5})]__##[ \\t]*\\r?\\n([^\\r\\n]+)[ \\t]*\\r?\\n##__C\\[\\1]__##[ \\t]*\\r?\\n(.*?)(?=^##__T\\[\\d{5}]__##|\\z)",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)
        )
        return pattern.findAll(text).mapIndexed { index, match ->
            UploadChapter(
                title = match.groupValues[2].trim().ifBlank { "Chapter ${index + 1}" },
                content = match.groupValues[3].trim(),
                chapterNumber = index + 1
            )
        }.toList()
    }

    private fun chaptersFromMatches(
        text: String,
        matches: List<MatchResult>,
        title: (MatchResult) -> String
    ): List<UploadChapter> {
        if (matches.isEmpty()) return emptyList()
        val preface = text.substring(0, matches.first().range.first).trim()
        return matches.mapIndexed { index, match ->
            val bodyStart = match.range.last + 1
            val bodyEnd = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val body = text.substring(bodyStart, bodyEnd).trim()
            val content = if (index == 0 && preface.isNotBlank()) listOf(preface, body).filter(String::isNotBlank).joinToString("\n\n") else body
            UploadChapter(
                title = title(match).ifBlank { "第 ${index + 1} 章" },
                content = content,
                chapterNumber = index + 1
            )
        }
    }
}
