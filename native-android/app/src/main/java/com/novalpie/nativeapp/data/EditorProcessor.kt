package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.UploadChapter

object EditorProcessor {
    private val titleIdentifier = Regex("^##__T\\[(\\d{5})]__##$", RegexOption.MULTILINE)
    private val contentIdentifier = Regex("^##__C\\[(\\d{5})]__##$", RegexOption.MULTILINE)
    private val titleToken = Regex("##__T\\[(\\d{5})]__##")
    private val contentToken = Regex("##__C\\[(\\d{5})]__##")

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

    /** Matches the website's “平均分章”: distribute complete paragraphs over a requested count. */
    fun splitEvenlyByChapterCount(text: String, targetChapterCount: Int): List<UploadChapter> {
        require(targetChapterCount > 0) { "Target chapter count must be positive" }
        val paragraphs = text
            .split(Regex("\\n\\s*\\n"))
            .map(String::trim)
            .filter(String::isNotEmpty)
        require(paragraphs.size >= targetChapterCount) {
            "Paragraph count (${paragraphs.size}) is below target chapter count ($targetChapterCount)"
        }
        val paragraphsPerChapter = (paragraphs.size + targetChapterCount - 1) / targetChapterCount
        return paragraphs
            .chunked(paragraphsPerChapter)
            .mapIndexed { index, chunk ->
                UploadChapter("第${index + 1}章", chunk.joinToString("\n\n"), index + 1)
            }
    }

    /** Matches the website's character interval rule, preferring the next paragraph break nearby. */
    fun splitByCharacterInterval(text: String, targetCharacters: Int): List<UploadChapter> {
        require(targetCharacters > 0) { "Target character count must be positive" }
        val chapters = mutableListOf<UploadChapter>()
        var start = 0
        while (start < text.length) {
            var end = (start + targetCharacters).coerceAtMost(text.length)
            if (end < text.length) {
                val paragraphBreak = text.indexOf("\n\n", end)
                if (paragraphBreak in end..(end + 200).coerceAtMost(text.length)) {
                    end = paragraphBreak + 2
                }
            }
            if (end <= start) break
            val content = text.substring(start, end).trim()
            if (content.isNotEmpty()) {
                chapters += UploadChapter("第${chapters.size + 1}章", content, chapters.size + 1)
            }
            start = end
        }
        return chapters
    }

    fun toWebsiteIdentifiers(chapters: List<UploadChapter>): String = chapters.joinToString("\n") { chapter ->
        val number = chapter.chapterNumber.coerceAtLeast(1).toString().padStart(5, '0')
        "##__T[$number]__##\n${chapter.title}\n##__C[$number]__##\n${chapter.content}"
    }

    fun clearWebsiteIdentifiers(text: String): String = text
        .replace(Regex("##__T\\[\\d{5}]__##\\s*"), "")
        .replace(Regex("##__C\\[\\d{5}]__##\\s*"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    /** Renumbers a valid source marker stream while retaining text outside chapter blocks. */
    fun renumberWebsiteIdentifiers(text: String): String {
        val validationErrors = validateWebsiteIdentifiers(text)
        require(validationErrors.isEmpty()) { validationErrors.joinToString("；") }
        val orderedNumbers = titleToken.findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
        if (orderedNumbers.isEmpty()) return text
        val replacementNumber = orderedNumbers.mapIndexed { index, oldNumber -> oldNumber to index + 1 }.toMap()
        val replacements = buildList {
            titleToken.findAll(text).forEach { match ->
                add(match to "##__T[${replacementNumber.getValue(match.groupValues[1]).toString().padStart(5, '0')}]__##")
            }
            contentToken.findAll(text).forEach { match ->
                add(match to "##__C[${replacementNumber.getValue(match.groupValues[1]).toString().padStart(5, '0')}]__##")
            }
        }
        return replacements
            .sortedByDescending { (match, _) -> match.range.first }
            .fold(text) { output, (match, replacement) -> output.replaceRange(match.range, replacement) }
    }

    /** Mirrors the source editor's standalone T button. */
    fun insertWebsiteTitleMarkerAtCursor(text: String, cursorPosition: Int): String {
        val number = nextWebsiteMarkerNumber(text)
        return insertWebsiteMarkerAtCursor(text, cursorPosition, "##__T[${number.toString().padStart(5, '0')}]__##")
    }

    /** Mirrors the source editor's standalone C button, pairing the nearest pending T marker. */
    fun insertWebsiteContentMarkerAtCursor(text: String, cursorPosition: Int): String {
        val cursor = cursorPosition.coerceIn(0, text.length)
        val allContentNumbers = contentToken.findAll(text).map { it.groupValues[1] }.toSet()
        val pendingTitleNumber = titleToken.findAll(text.substring(0, cursor))
            .map { it.groupValues[1] }
            .lastOrNull { it !in allContentNumbers }
        val number = pendingTitleNumber?.toIntOrNull() ?: nextWebsiteMarkerNumber(text)
        return insertWebsiteMarkerAtCursor(text, cursor, "##__C[${number.toString().padStart(5, '0')}]__##")
    }

    /** The source blade tool inserts a complete T/C pair at the current cursor and then renumbers. */
    fun insertWebsiteChapterAtCursor(text: String, cursorPosition: Int, title: String? = null): String {
        val nextNumber = (titleToken.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull() ?: 0) + 1
        val cursor = cursorPosition.coerceIn(0, text.length)
        var before = text.substring(0, cursor)
        var after = text.substring(cursor)
        if (before.isNotEmpty() && !before.endsWith("\n")) before += "\n"
        if (after.isNotEmpty() && !after.startsWith("\n")) after = "\n$after"
        val marker = "##__T[${nextNumber.toString().padStart(5, '0')}]__##\n${title?.trim().orEmpty().ifBlank { "第${nextNumber}章" }}\n##__C[${nextNumber.toString().padStart(5, '0')}]__##"
        return renumberWebsiteIdentifiers(before + marker + after)
    }

    /** Deletes the source chapter whose title marker precedes the current cursor, then renumbers. */
    fun deleteWebsiteChapterAtCursor(text: String, cursorPosition: Int): String {
        val validationErrors = validateWebsiteIdentifiers(text)
        require(validationErrors.isEmpty()) { validationErrors.joinToString("；") }
        val titles = titleToken.findAll(text).toList()
        val cursor = cursorPosition.coerceIn(0, text.length)
        val selectedIndex = titles.indexOfLast { match -> match.range.first <= cursor }
        require(selectedIndex >= 0) { "Place the cursor inside a chapter before deleting it" }
        val start = titles[selectedIndex].range.first
        val end = titles.getOrNull(selectedIndex + 1)?.range?.first ?: text.length
        val before = text.substring(0, start).trimEnd()
        val after = text.substring(end).trimStart()
        val remaining = when {
            before.isBlank() -> after
            after.isBlank() -> before
            else -> "$before\n\n$after"
        }
        return if (remaining.isBlank()) "" else renumberWebsiteIdentifiers(remaining)
    }

    private fun nextWebsiteMarkerNumber(text: String): Int =
        (titleToken.findAll(text).asSequence() + contentToken.findAll(text).asSequence())
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?: 1

    private fun insertWebsiteMarkerAtCursor(text: String, cursorPosition: Int, marker: String): String {
        val cursor = cursorPosition.coerceIn(0, text.length)
        val before = text.substring(0, cursor)
        val after = text.substring(cursor)
        val prefix = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        val suffix = if (after.isEmpty() || after.startsWith("\n")) "" else "\n"
        return before + prefix + marker + suffix + after
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
