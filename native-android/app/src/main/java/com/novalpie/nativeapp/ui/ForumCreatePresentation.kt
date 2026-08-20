package com.novalpie.nativeapp.ui

internal data class ForumCategoryOption(
    val id: String,
    val title: String,
    val description: String
)

data class ForumCreateDraft(
    val type: String = "",
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val tagDraft: String = "",
    val pollEnabled: Boolean = false,
    val pollQuestion: String = "",
    val pollOptions: List<String> = listOf("", ""),
    val pollAllowMultiple: Boolean = false,
    val pollMaxChoices: Int = 2,
    val pollEndsAt: String = ""
)

internal data class ForumCreateValidation(
    val canSubmit: Boolean,
    val message: String? = null
)

internal fun forumCategoryOptions(isAdmin: Boolean): List<ForumCategoryOption> = buildList {
    if (isAdmin) {
        add(ForumCategoryOption("announcement", "公告", "站务公告，仅管理员可以发布"))
    }
    add(ForumCategoryOption("recommend", "推书", "分享喜欢的作品，详细介绍会更有帮助"))
    add(ForumCategoryOption("discussion", "交流", "聊剧情、设定、吐槽或求书"))
    add(ForumCategoryOption("feedback", "反馈", "反馈问题或建议，并附复现步骤、错误信息和链接"))
}

internal fun validateForumCreateDraft(
    draft: ForumCreateDraft,
    isAdmin: Boolean
): ForumCreateValidation {
    val allowedTypes = forumCategoryOptions(isAdmin).mapTo(mutableSetOf()) { it.id }
    if (draft.type == "announcement" && !isAdmin) {
        return ForumCreateValidation(false, "只有管理员可以发布公告")
    }
    if (draft.type !in allowedTypes) return ForumCreateValidation(false, "请选择发布分区")
    val title = draft.title.trim()
    if (title.isEmpty()) return ForumCreateValidation(false, "请输入帖子标题")
    if (title.length > 100) return ForumCreateValidation(false, "标题不能超过 100 个字符")
    val content = draft.content.trim()
    if (content.isEmpty()) return ForumCreateValidation(false, "请输入帖子内容")
    if (content.length > 10_000) return ForumCreateValidation(false, "内容不能超过 10000 个字符")
    val tags = draft.tags.map(String::trim).filter(String::isNotEmpty)
    if (tags.size > 5) return ForumCreateValidation(false, "最多添加 5 个标签")
    if (tags.any { it.length > 20 }) return ForumCreateValidation(false, "单个标签不能超过 20 个字符")
    if (tags.distinct().size != tags.size) return ForumCreateValidation(false, "标签不能重复")
    if (!draft.pollEnabled) return ForumCreateValidation(true)

    val options = draft.pollOptions.map(String::trim).filter(String::isNotEmpty)
    if (options.size < 2) return ForumCreateValidation(false, "至少填写 2 个投票选项")
    if (options.size > 10) return ForumCreateValidation(false, "投票选项不能超过 10 个")
    if (options.distinct().size != options.size) return ForumCreateValidation(false, "投票选项不能重复")
    if (draft.pollAllowMultiple && draft.pollMaxChoices !in 2..options.size) {
        return ForumCreateValidation(false, "多选上限应为 2 到 ${options.size}")
    }
    return ForumCreateValidation(true)
}
