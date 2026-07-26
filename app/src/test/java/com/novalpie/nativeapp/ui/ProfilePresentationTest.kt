package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserCheckinStats
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePresentationTest {
    @Test
    fun websiteProfileFactsIncludePointsActivityAndCheckinStreak() {
        val facts = profileWebsiteFacts(
            profile = UserProfile(
                id = 7,
                name = "seeking",
                role = "admin",
                points = 3210,
                stats = mapOf("novels" to 4, "comments" to 29, "followers" to 8)
            ),
            checkinStats = UserCheckinStats(totalDays = 20, totalPoints = 240, maxStreak = 7, currentStreak = 3)
        )

        assertEquals(listOf("积分 3210", "作品 4", "评论 29", "连续签到 3 天"), facts)
    }

    @Test
    fun administratorRoleRequiresExactWebsiteRoleValue() {
        assertTrue(isAdminProfile(UserProfile(1, "Exact", role = "admin")))
        assertFalse(isAdminProfile(UserProfile(2, "Uppercase", role = "ADMIN")))
        assertFalse(isAdminProfile(UserProfile(3, "Similar", role = "administrator")))
        assertFalse(isAdminProfile(null))
    }

    @Test
    fun profileAccountStatusLabelsExposeWebsiteAccountState() {
        val active = UserProfile(
            id = 7,
            name = "seeking",
            isBanned = false,
            isAdult = true,
            email = "profile@example.test",
            createdAt = "2026-01-02T03:04:05Z",
            showCheckin = true,
            autoCheckin = false
        )
        val banned = UserProfile(
            id = 8,
            name = "blocked",
            isBanned = true,
            banReason = "spam",
            banExpiresAt = "2026-08-09T00:00:00Z",
            isAdult = false,
            deleted = false
        )

        assertEquals(
            listOf("账号正常", "成年已验证", "邮箱已绑定", "注册 2026-01-02", "签到公开", "自动签到未开"),
            profileAccountStatusLabels(active)
        )
        assertEquals(
            listOf("账号封禁至 2026-08-09", "封禁原因 spam", "成年未验证"),
            profileAccountStatusLabels(banned)
        )
        assertEquals(listOf("账号已删除"), profileAccountStatusLabels(UserProfile(id = 9, name = "deleted", deleted = true)))
    }

    @Test
    fun profileOverviewUsesUserCenterLanguage() {
        val overview = profileOverview(
            user = LoadResult.Success(UserProfile(id = 7, name = "seeking", role = "admin")),
            hasAuthToken = true,
            readerProgress = ReaderProgress(bookId = 354491, chapterId = 8001, chapterTitle = "第一章"),
            readerOptions = ReaderUiOptions(fontSizeSp = 19, theme = "sepia"),
            proxyEnabled = true
        )

        assertEquals("我的", overview.title)
        assertEquals("账号、阅读偏好和连接设置", overview.subtitle)
        assertEquals("seeking", overview.accountName)
        assertEquals("已同步", overview.syncLabel)
        assertEquals("管理员", overview.roleLabel)
        assertEquals(listOf("阅读 章节 8001", "字号 19sp", "主题 护眼", "连接 已启用"), overview.stats)
    }

    @Test
    fun profileOverviewShowsGuestStateCleanly() {
        val overview = profileOverview(
            user = LoadResult.Error("missing token"),
            hasAuthToken = false,
            readerProgress = null,
            readerOptions = ReaderUiOptions(),
            proxyEnabled = false
        )

        assertEquals("未登录", overview.accountName)
        assertEquals("未同步", overview.syncLabel)
        assertEquals("普通用户", overview.roleLabel)
        assertEquals(listOf("阅读 无进度", "字号 18sp", "主题 系统", "连接 未启用"), overview.stats)
    }

    @Test
    fun profileOverviewDoesNotCallASavedSessionLoggedOutDuringRefreshFailure() {
        val overview = profileOverview(
            user = LoadResult.Error("timeout"),
            hasAuthToken = true,
            readerProgress = null,
            readerOptions = ReaderUiOptions(),
            proxyEnabled = false
        )

        assertEquals("账号已同步", overview.accountName)
        assertEquals("已同步", overview.syncLabel)
        assertEquals("身份待同步", overview.roleLabel)
    }

    @Test
    fun cachedTokenProfileSurvivesRemoteProfileTimeout() {
        val cached = UserProfile(id = 100000, name = "seeking", role = "admin")

        val resolved = resolveUserLoadResult(
            remote = Result.failure(IOException("timeout")),
            tokenProfile = cached
        )

        assertEquals(LoadResult.Success(cached), resolved)
    }

    @Test
    fun profileSectionsStayProductFacing() {
        assertEquals(listOf("账号", "阅读偏好", "连接设置", "网页入口"), profileSectionTitles())
        assertEquals(listOf("同步账号", "网页登录", "退出同步"), profileAccountActions(hasAuthToken = true))
        assertEquals(listOf("同步账号", "网页登录"), profileAccountActions(hasAuthToken = false))
        assertEquals(listOf("打开网站", "网页搜索"), profileWebActions())
    }

    @Test
    fun profileCopyDoesNotExposeDebugOrUnsupportedReaderTooling() {
        val forbidden = listOf("Package", "role:", "API", "诊断", "书源", "规则", "爬取", "下载", "净化", "编辑源")
        val overview = profileOverview(
            user = LoadResult.Success(UserProfile(id = 7, name = "seeking", role = "admin")),
            hasAuthToken = true,
            readerProgress = null,
            readerOptions = ReaderUiOptions(),
            proxyEnabled = true
        )
        val copy = listOf(overview.title, overview.subtitle, overview.accountName, overview.syncLabel, overview.roleLabel)
            .plus(overview.stats)
            .plus(profileAccountStatusLabels(UserProfile(id = 7, name = "seeking", isBanned = false, isAdult = true)))
            .plus(profileSectionTitles())
            .plus(profileAccountActions(hasAuthToken = true))
            .plus(profileWebActions())

        copy.forEach { value ->
            forbidden.forEach { word ->
                assertFalse("Profile copy should not expose '$word': $value", value.contains(word, ignoreCase = true))
            }
        }
    }
}
