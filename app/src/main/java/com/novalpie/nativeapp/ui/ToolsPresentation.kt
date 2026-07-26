package com.novalpie.nativeapp.ui

internal data class ToolEntry(
    val title: String,
    val subtitle: String,
    val path: String,
    val adminOnly: Boolean = false
)

internal fun toolsEntries(isAdmin: Boolean): List<ToolEntry> {
    val core = listOf(
        ToolEntry(
            title = "\u6d88\u606f\u4e2d\u5fc3",
            subtitle = "\u901a\u77e5\u3001\u79c1\u4fe1\u4e0e\u7528\u6237\u4e92\u52a8",
            path = "/messages"
        ),
        ToolEntry(
            title = "\u5de5\u4f5c\u533a",
            subtitle = "\u7ffb\u8bd1\u63a5\u53e3\u3001Cookie \u4e0e\u670d\u52a1\u72b6\u6001",
            path = "/workspace"
        ),
        ToolEntry(
            title = "\u4e0a\u4f20\u4e66\u7c4d",
            subtitle = "\u5bfc\u5165 EPUB \u5e76\u63d0\u4ea4\u5230\u7f51\u7ad9",
            path = "/upload"
        ),
        ToolEntry(
            title = "\u4e0a\u4f20\u7f16\u8f91\u5668",
            subtitle = "\u5206\u7ae0\u3001\u66ff\u6362\u3001AI \u6b63\u5219\u4e0e\u8349\u7a3f",
            path = "/upload-editor"
        ),
        ToolEntry(
            title = "\u653f\u6cbb\u8003\u8bd5",
            subtitle = "\u7f51\u7ad9\u79ef\u5206\u5956\u52b1\u5165\u53e3",
            path = "/political-exam"
        )
    )
    if (!isAdmin) return core

    return core + listOf(
        ToolEntry("\u7ba1\u7406\u540e\u53f0", "\u7ba1\u7406\u5458\u529f\u80fd\u603b\u89c8", "/admin", adminOnly = true),
        ToolEntry("\u5185\u5bb9\u5ba1\u6838", "\u5ba1\u6838\u4e0e\u5185\u5bb9\u5904\u7406", "/admin/review", adminOnly = true),
        ToolEntry("\u5bc6\u94a5\u7ba1\u7406", "API \u5bc6\u94a5\u4e0e\u4f7f\u7528\u72b6\u6001", "/admin/key-management", adminOnly = true),
        ToolEntry("\u64cd\u4f5c\u65e5\u5fd7", "\u7ba1\u7406\u64cd\u4f5c\u8bb0\u5f55", "/admin/operation-logs", adminOnly = true),
        ToolEntry("\u6293\u53d6\u7ba1\u7406", "\u6293\u53d6\u5668\u4e0e\u4efb\u52a1\u72b6\u6001", "/admin/scraper-management", adminOnly = true),
        ToolEntry("\u5546\u5e97\u7ba1\u7406", "\u7ad9\u5185\u5546\u5e97\u914d\u7f6e", "/admin/shop", adminOnly = true)
    )
}

internal fun messageTypeLabel(type: Int?): String = when (type) {
    null -> "\u5168\u90e8\u7c7b\u578b"
    1 -> "\u7528\u6237\u4e92\u52a8"
    2 -> "\u5e16\u5b50\u56de\u590d"
    3 -> "\u7cfb\u7edf\u901a\u77e5"
    4 -> "\u5c0f\u8bf4\u66f4\u65b0"
    5 -> "\u8bc4\u8bba\u56de\u590d"
    6 -> "\u70b9\u8d5e\u901a\u77e5"
    7 -> "\u5173\u6ce8\u901a\u77e5"
    8 -> "\u79c1\u4fe1"
    9 -> "\u7cfb\u7edf\u516c\u544a"
    10 -> "\u4e3e\u62a5\u901a\u77e5"
    else -> "\u672a\u77e5\u7c7b\u578b"
}
