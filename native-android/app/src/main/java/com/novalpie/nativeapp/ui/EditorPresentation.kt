package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.UploadChapter

/**
 * A bounded, in-memory history for the editor document only.
 *
 * It deliberately keeps editor history out of archives and avoids retaining a
 * chain of large EPUB-sized strings. A new document replaces the history.
 */
data class EditorDocumentSnapshot(
    val text: String,
    val cursorPosition: Int,
    val chapters: List<UploadChapter>,
    val markerValidationErrors: List<String>
)

class EditorDocumentHistory(
    private val maxEntries: Int = 64,
    private val maxCharacters: Int = 1_000_000
) {
    private val undoStack = ArrayDeque<EditorDocumentSnapshot>()
    private val redoStack = ArrayDeque<EditorDocumentSnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(before: EditorDocumentSnapshot, after: EditorDocumentSnapshot) {
        if (before == after) return
        redoStack.clear()
        if (snapshotCharacters(before) > maxCharacters || snapshotCharacters(after) > maxCharacters) {
            undoStack.clear()
            return
        }
        undoStack.addLast(before)
        trim()
    }

    fun undo(current: EditorDocumentSnapshot): EditorDocumentSnapshot? {
        if (undoStack.isEmpty()) return null
        val previous = undoStack.removeLast()
        if (snapshotCharacters(current) <= maxCharacters) redoStack.addLast(current)
        trim()
        return previous
    }

    fun redo(current: EditorDocumentSnapshot): EditorDocumentSnapshot? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeLast()
        if (snapshotCharacters(current) <= maxCharacters) undoStack.addLast(current)
        trim()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    private fun trim() {
        while (undoStack.size + redoStack.size > maxEntries) {
            if (undoStack.isNotEmpty()) undoStack.removeFirst() else redoStack.removeFirst()
        }
        while (historyCharacters() > maxCharacters) {
            if (undoStack.isNotEmpty()) undoStack.removeFirst()
            else if (redoStack.isNotEmpty()) redoStack.removeFirst()
            else return
        }
    }

    private fun historyCharacters(): Int =
        (undoStack.asSequence() + redoStack.asSequence()).sumOf(::snapshotCharacters)

    private fun snapshotCharacters(snapshot: EditorDocumentSnapshot): Int =
        snapshot.text.length + snapshot.chapters.sumOf { it.title.length + it.content.length }
}

enum class EditorTab(val label: String) {
    Files("文件"),
    Text("文本"),
    Split("分章"),
    Chapters("目录"),
    Metadata("书籍"),
    Archives("存档")
}

enum class EditorSplitMode(val label: String) {
    Regex("正则表达式"),
    MarkdownH1("Markdown 一级标题"),
    MarkdownH2("Markdown 二级标题"),
    KeywordNumber("关键词 + 数字"),
    CharacterCount("按字数"),
    ParagraphCount("按段落数"),
    ApiProcess("接口处理"),
    BatchGenerate("批量生成"),
    CustomScript("自定义脚本"),
    Manual("手动工具")
}

/** Mirrors the website's marker policy without persisting an external processor endpoint. */
enum class EditorMarkerMode(val label: String) {
    Incremental("增量模式"),
    Full("全新模式")
}

/** The three source batch-generation rules use one shared numeric target. */
enum class EditorBatchMode(val label: String, val defaultTarget: String) {
    Paragraphs("按段落数", "10"),
    Characters("按字数", "3000"),
    Chapters("平均分章", "100")
}

val DEFAULT_EDITOR_CHAPTER_REGEX = "^第[\\d零一二三四五六七八九十百千万]+章.*$"

val DEFAULT_EDITOR_CUSTOM_SCRIPT = """
    function processText(text, options) {
      return text;
    }
""".trimIndent()

fun editorSplitTargetError(
    mode: EditorSplitMode,
    pattern: String,
    target: String,
    customScript: String = "",
    scriptChunked: Boolean = false,
    scriptChunkSize: String = ""
): String? = when (mode) {
    EditorSplitMode.Regex -> if (pattern.lineSequence().none { it.isNotBlank() }) "请输入至少一个正则表达式" else null
    EditorSplitMode.KeywordNumber -> if (pattern.lineSequence().none { it.isNotBlank() }) "请输入至少一个关键词" else null
    EditorSplitMode.CharacterCount,
    EditorSplitMode.ParagraphCount -> if ((target.toIntOrNull() ?: 0) <= 0) "分块目标必须大于 0" else null
    EditorSplitMode.ApiProcess -> when {
        pattern.isBlank() -> "请输入接口地址"
        (target.toIntOrNull() ?: 0) !in 1..120 -> "请求超时必须介于 1 到 120 秒"
        else -> null
    }
    EditorSplitMode.BatchGenerate -> if ((target.toIntOrNull() ?: 0) <= 0) "分章目标必须大于 0" else null
    EditorSplitMode.CustomScript -> when {
        customScript.isBlank() -> "请输入 processText JavaScript 脚本"
        !customScript.contains("processText") -> "脚本必须定义 processText(text, options)"
        scriptChunked && (scriptChunkSize.toIntOrNull() ?: 0) !in 1_024..1_000_000 -> "分块大小必须介于 1024 到 1000000 字符"
        else -> null
    }
    EditorSplitMode.MarkdownH1,
    EditorSplitMode.MarkdownH2,
    EditorSplitMode.Manual -> null
}
