package com.novalpie.nativeapp.ui

enum class EditorTab(val label: String) {
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
    CustomScript("自定义脚本")
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
    EditorSplitMode.CustomScript -> when {
        customScript.isBlank() -> "请输入 processText JavaScript 脚本"
        !customScript.contains("processText") -> "脚本必须定义 processText(text, options)"
        scriptChunked && (scriptChunkSize.toIntOrNull() ?: 0) !in 1_024..1_000_000 -> "分块大小必须介于 1024 到 1000000 字符"
        else -> null
    }
    else -> null
}
