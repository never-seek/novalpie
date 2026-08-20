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
            title = "消息中心",
            subtitle = "通知、私信与用户互动",
            path = "/messages"
        ),
        ToolEntry(
            title = "工作区",
            subtitle = "翻译接口、Cookie 与服务状态",
            path = "/workspace"
        ),
        ToolEntry(
            title = "上传书籍",
            subtitle = "导入 EPUB 并提交到网站",
            path = "/upload"
        ),
        ToolEntry(
            title = "上传编辑器",
            subtitle = "分章、替换、AI 正则与草稿",
            path = "/upload-editor"
        ),
        ToolEntry(
            title = "政治考试",
            subtitle = "网站积分奖励入口",
            path = "/political-exam"
        )
    )
    if (!isAdmin) return core

    return core + listOf(
        ToolEntry("管理后台", "管理员功能总览", "/admin", adminOnly = true),
        ToolEntry("内容审核", "审核与内容处理", "/admin/review", adminOnly = true),
        ToolEntry("密钥管理", "API 密钥与使用状态", "/admin/key-management", adminOnly = true),
        ToolEntry("操作日志", "管理操作记录", "/admin/operation-logs", adminOnly = true),
        ToolEntry("抓取管理", "抓取器与任务状态", "/admin/scraper-management", adminOnly = true),
        ToolEntry("商店管理", "站内商店配置", "/admin/shop", adminOnly = true)
    )
}

internal fun messageTypeLabel(type: Int?): String = when (type) {
    null -> "全部类型"
    1 -> "用户互动"
    2 -> "帖子回复"
    3 -> "系统通知"
    4 -> "小说更新"
    5 -> "评论回复"
    6 -> "点赞通知"
    7 -> "关注通知"
    8 -> "私信"
    9 -> "系统公告"
    10 -> "举报通知"
    else -> "未知类型"
}
