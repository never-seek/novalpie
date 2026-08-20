package com.novalpie.nativeapp.ui

internal object VisibleUiLabels {
    const val ForumPostDetail = "帖子详情"
    const val Comments = "评论"
    const val CommentSubmit = "评论提交"
    const val FavoriteGroups = "收藏分组"
    const val Bookshelf = "书架"
    const val Search = "搜索"
    const val BookDetail = "书籍详情"
    const val ChapterCatalog = "章节目录"
    const val ChapterComments = "章节评论"
}

internal fun forumPostActionLabel(action: ForumPostAction): String = when (action) {
    ForumPostAction.Like -> "点赞"
    ForumPostAction.Dislike -> "点踩"
    ForumPostAction.Emoji -> "表情"
    ForumPostAction.Award -> "打赏"
}

internal fun forumCommentActionLabel(action: ForumPostAction): String =
    "评论${forumPostActionLabel(action)}"

internal enum class ForumPostAction {
    Like,
    Dislike,
    Emoji,
    Award
}
