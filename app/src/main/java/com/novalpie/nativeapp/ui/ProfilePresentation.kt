package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserCheckinStats

internal fun isAdminProfile(profile: UserProfile?): Boolean = profile?.role == "admin"

internal fun profileWebsiteFacts(
    profile: UserProfile,
    checkinStats: UserCheckinStats?
): List<String> = listOf(
    "积分 ${profile.points ?: 0}",
    "作品 ${profile.stats["novels"] ?: 0}",
    "评论 ${profile.stats["comments"] ?: 0}",
    "连续签到 ${checkinStats?.currentStreak ?: 0} 天"
)

internal fun profileAccountStatusLabels(profile: UserProfile): List<String> = buildList {
    if (profile.deleted == true) {
        add("账号已删除")
    } else if (profile.isBanned == true) {
        add(profile.banExpiresAt?.takeIf(String::isNotBlank)?.let { "账号封禁至 ${profileShortDate(it)}" } ?: "账号封禁")
        profile.banReason?.takeIf(String::isNotBlank)?.let { add("封禁原因 $it") }
    } else if (profile.isBanned == false) {
        add("账号正常")
    }
    when (profile.isAdult) {
        true -> add("成年已验证")
        false -> add("成年未验证")
        null -> Unit
    }
    if (!profile.email.isNullOrBlank()) add("邮箱已绑定")
    profile.createdAt?.takeIf(String::isNotBlank)?.let { add("注册 ${profileShortDate(it)}") }
    profile.showCheckin?.let { add(if (it) "签到公开" else "签到隐藏") }
    profile.autoCheckin?.let { add(if (it) "自动签到已开" else "自动签到未开") }
}

private fun profileShortDate(value: String): String =
    value.takeIf { it.length >= 10 && it[4] == '-' && it[7] == '-' }?.take(10) ?: value

internal data class ProfileOverview(
    val title: String,
    val subtitle: String,
    val accountName: String,
    val syncLabel: String,
    val roleLabel: String,
    val stats: List<String>
)

internal fun profileOverview(
    user: LoadResult<UserProfile>,
    hasAuthToken: Boolean,
    readerProgress: ReaderProgress?,
    readerOptions: ReaderUiOptions,
    proxyEnabled: Boolean
): ProfileOverview {
    val header = productHeader(ProductSurface.Profile)
    val profile = (user as? LoadResult.Success)?.value
    return ProfileOverview(
        title = header.title,
        subtitle = header.subtitle,
        accountName = profile?.name ?: if (hasAuthToken) "账号已同步" else "未登录",
        syncLabel = if (hasAuthToken) "已同步" else "未同步",
        roleLabel = profileRoleLabel(profile?.role, hasAuthToken),
        stats = listOf(
            readerProgress?.let { "阅读 章节 ${it.chapterId}" } ?: "阅读 无进度",
            "字号 ${readerOptions.fontSizeSp}sp",
            "主题 ${readerOptions.theme.themeLabel()}",
            if (proxyEnabled) "连接 已启用" else "连接 未启用"
        )
    )
}

internal fun profileSectionTitles(): List<String> =
    listOf("账号", "阅读偏好", "连接设置", "网页入口")

internal fun profileAccountActions(hasAuthToken: Boolean): List<String> =
    if (hasAuthToken) {
        listOf("同步账号", "网页登录", "退出同步")
    } else {
        listOf("同步账号", "网页登录")
    }

internal fun profileWebActions(): List<String> =
    listOf("打开网站", "网页搜索")

private fun profileRoleLabel(role: String?, hasAuthToken: Boolean): String =
    when {
        role == "admin" -> "管理员"
        !role.isNullOrBlank() -> "普通用户"
        hasAuthToken -> "身份待同步"
        else -> "普通用户"
    }
