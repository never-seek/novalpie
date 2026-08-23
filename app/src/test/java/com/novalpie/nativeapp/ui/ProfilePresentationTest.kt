package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.data.NovalPieApiException
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserContentActivityFeed
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.UserInventory
import com.novalpie.nativeapp.model.UserInventoryItem
import com.novalpie.nativeapp.model.UserBadge
import com.novalpie.nativeapp.model.UserQuizRewardStatus
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePresentationTest {
    @Test
    fun activityFiltersMirrorTheWebsiteProfileCategories() {
        val activities = listOf(
            UserActivity(1, "post", "帖子"),
            UserActivity(2, "post_comment", "论坛评论"),
            UserActivity(3, "novel_comment", "书评"),
            UserActivity(4, "chapter_comment", "章评"),
        )

        assertEquals(listOf("全部", "帖子", "评论", "书评", "章评"), ProfileActivityFilter.values().map(::profileActivityFilterLabel))
        assertEquals(activities, filterProfileActivities(activities, ProfileActivityFilter.All))
        assertEquals(listOf(1L), filterProfileActivities(activities, ProfileActivityFilter.Posts).map { it.id })
        assertEquals(listOf(2L), filterProfileActivities(activities, ProfileActivityFilter.Comments).map { it.id })
        assertEquals(listOf(3L), filterProfileActivities(activities, ProfileActivityFilter.BookReviews).map { it.id })
        assertEquals(listOf(4L), filterProfileActivities(activities, ProfileActivityFilter.ChapterReviews).map { it.id })
    }

    @Test
    fun uploadedBookSearchMatchesTitleAuthorAndTagsLocally() {
        val books = listOf(
            NovelCard(id = 1, title = "Sky Archive", author = "Aster", tags = listOf("fantasy")),
            NovelCard(id = 2, title = "Signal Fire", author = "Mori", tags = listOf("drama", "romance"))
        )

        assertEquals(listOf(1L), filterBooks(books, "archive").map { it.id })
        assertEquals(listOf(2L), filterBooks(books, "mori").map { it.id })
        assertEquals(listOf(2L), filterBooks(books, "romance").map { it.id })
        assertEquals(books, filterBooks(books, "   "))
    }

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
    fun profileUsesSourceContentTotalsWhenProfileStatsAreOmitted() {
        val profile = profileWithContentActivityCounts(
            UserProfile(id = 7, name = "seeking"),
            UserContentActivityFeed(postCount = 4, forumCommentCount = 127, bookReviewCount = 21)
        )

        assertEquals(4L, profile?.stats?.get("posts"))
        assertEquals(148L, profile?.stats?.get("comments"))
        assertEquals(
            9L,
            profileWithContentActivityCounts(
                UserProfile(id = 7, name = "seeking", stats = mapOf("posts" to 9L)),
                UserContentActivityFeed(postCount = 4, forumCommentCount = 127, bookReviewCount = 21)
            )?.stats?.get("posts")
        )
    }

    @Test
    fun publicProfileLoadPresentationKeepsActivityFeedCountersAndEntriesTogether() {
        val activity = UserActivity(id = 91, type = "post_comment", title = "交流区")
        val presentation = publicProfileLoadPresentation(
            profile = UserProfile(id = 100002, name = "榛名全色"),
            feed = UserContentActivityFeed(
                activities = listOf(activity),
                postCount = 4,
                forumCommentCount = 127,
                bookReviewCount = 21,
            ),
            novels = emptyList(),
        )

        assertEquals(listOf(activity), presentation.activities)
        assertEquals(148L, presentation.profile?.stats?.get("comments"))
        assertEquals(0L, presentation.profile?.stats?.get("novels"))
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
        assertEquals(listOf("阅读 无进度", "字号 16sp", "主题 系统", "连接 未启用"), overview.stats)
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
    fun tokenOnlyProfileDoesNotPretendToContainRemoteProfileFacts() {
        val tokenProfile = UserProfile(id = 100000, name = "seeking", role = "admin")

        assertTrue(tokenProfile.stats.isEmpty())
        assertEquals(null, tokenProfile.points)
        assertEquals(null, tokenProfile.avatarUrl)
    }

    @Test
    fun profileMetricLabelsKeepUnknownValuesDistinctFromRealZeroes() {
        assertEquals("—", profileMetricValueLabel(null))
        assertEquals("0", profileMetricValueLabel(0L))
        assertEquals("42", profileMetricValueLabel(42L))
    }

    @Test
    fun profileSectionsStayProductFacing() {
        assertEquals(listOf("账号", "阅读偏好", "连接设置", "网页入口"), profileSectionTitles())
        assertEquals(listOf("同步账号", "网页登录", "退出同步"), profileAccountActions(hasAuthToken = true))
        assertEquals(listOf("同步账号", "网页登录"), profileAccountActions(hasAuthToken = false))
        assertEquals(listOf("打开网站", "网页搜索"), profileWebActions())
    }

    @Test
    fun quizRewardCopyDistinguishesClaimedEligibleAndUnknownStates() {
        assertEquals("奖励已领取", profileQuizRewardLabel(UserQuizRewardStatus(claimed = true)))
        assertEquals("奖励问答可领取", profileQuizRewardLabel(UserQuizRewardStatus(claimed = false, eligible = true)))
        assertEquals("暂不满足奖励条件", profileQuizRewardLabel(UserQuizRewardStatus(eligible = false)))
        assertEquals("奖励状态待确认", profileQuizRewardLabel(UserQuizRewardStatus()))
    }

    @Test
    fun zeroedCheckinSummaryUsesVisibleSourceRecordsAsFallback() {
        val reconciled = reconcileCheckinStats(
            source = UserCheckinStats(),
            records = listOf(
                UserCheckinRecord("2026-08-08", points = 5),
                UserCheckinRecord("2026-08-09", points = 5)
            ),
            today = "2026-08-09"
        )

        assertEquals(2, reconciled.totalDays)
        assertEquals(10L, reconciled.totalPoints)
        assertEquals(2, reconciled.maxStreak)
        assertEquals(2, reconciled.currentStreak)
    }

    @Test
    fun explicitZeroActivityCountersPermitAnEmptyStateButUnknownCountersDoNot() {
        assertTrue(
            profileHasNoPublicActivities(
                UserProfile(id = 7, name = "quiet", stats = mapOf("posts" to 0L, "comments" to 0L))
            )
        )
        assertFalse(profileHasNoPublicActivities(UserProfile(id = 8, name = "unknown")))
        assertFalse(
            profileHasNoPublicActivities(
                UserProfile(id = 9, name = "writer", stats = mapOf("posts" to 1L, "comments" to 0L))
            )
        )
    }

    @Test
    fun equippedInventoryFrameBackfillsOnlyAMissingProfileFrame() {
        val inventory = UserInventory(
            items = listOf(
                UserInventoryItem(
                    id = 17,
                    name = "Maid Frame",
                    type = "avatar_frame",
                    imageUrl = "https://images.novelpia.com/frames/maid.webp",
                    equipped = true
                )
            )
        )
        val profile = UserProfile(id = 7, name = "reader")

        assertEquals(
            "https://images.novelpia.com/frames/maid.webp",
            profileWithEquippedAvatarFrame(profile, inventory)?.avatarFrameUrl
        )
        assertEquals(
            "https://images.novelpia.com/frames/source.webp",
            profileWithEquippedAvatarFrame(
                profile.copy(avatarFrameUrl = "https://images.novelpia.com/frames/source.webp"),
                inventory
        )?.avatarFrameUrl
        )
    }

    @Test
    fun equippedBadgeBackfillsProfileAndKeepsItsOwnSourceStyle() {
        val inventory = UserInventory(
            items = listOf(
                UserInventoryItem(
                    id = 17,
                    itemId = 51,
                    name = "Aurora",
                    type = "badge",
                    description = "Source Badge",
                    badgeHtml = "<span class=\"badge\"><span class=\"badge__dot\"></span>{{name}}</span>",
                    badgeCss = "--bg: linear-gradient(135deg, #22d3ee, #a855f7); background: var(--bg);",
                    equipped = true,
                ),
                UserInventoryItem(id = 18, itemId = 52, name = "Unequipped", type = "badge", equipped = false),
            ),
        )
        val profile = UserProfile(
            id = 7,
            name = "reader",
            badges = listOf(UserBadge(id = 51, name = "Aurora")),
        )

        val merged = profileWithEquippedCosmetics(profile, inventory)!!

        assertEquals(1, merged.badges.size)
        assertEquals("Aurora", merged.badges.single().name)
        assertTrue(merged.badges.single().badgeCss?.contains("#22d3ee") == true)
        assertTrue(merged.badges.single().badgeHtml?.contains("badge__dot") == true)
    }

    @Test
    fun avatarFrameUsesTheWebsiteOuterOverlayScale() {
        assertEquals(1.7f, PROFILE_AVATAR_FRAME_SCALE)
    }

    @Test
    fun profileBadgeLayoutUsesTheLiveMdHeaderAndBackpackContract() {
        assertEquals(6, PROFILE_HEADER_BADGE_MAX)
        assertEquals(
            ProfileBadgeVisualSpec(
                heightDp = 22,
                maxRadiusDp = 11,
                horizontalPaddingDp = 9,
                dotSizeDp = 6,
                fontSizeSp = 12,
                contentGapDp = 5,
                shadowDp = 6,
            ),
            profileBadgeVisualSpec(ProfileBadgeDisplay.Hero),
        )
        assertEquals(
            profileBadgeVisualSpec(ProfileBadgeDisplay.Hero),
            profileBadgeVisualSpec(ProfileBadgeDisplay.Showcase),
        )
    }

    @Test
    fun badgeRendererPreservesSafeSourceGeometryForArtworkBadges() {
        val metrics = profileBadgeRenderMetrics(
            css = """
                .badge {
                  background-image: url('https://images.novelpia.com/badges/source.webp');
                  width: 125px;
                  height: 34px;
                  padding-left: 15px;
                  font-size: 15px;
                }
            """.trimIndent(),
            display = ProfileBadgeDisplay.Inline,
        )

        assertEquals(
            ProfileBadgeRenderMetrics(
                widthDp = 125,
                heightDp = 34,
                startPaddingDp = 15,
                endPaddingDp = 7,
                fontSizeSp = 15,
            ),
            metrics,
        )
    }

    @Test
    fun badgeRendererKeepsTheStandardPillWhenSourceDoesNotDeclareGeometry() {
        assertEquals(
            ProfileBadgeRenderMetrics(
                widthDp = null,
                heightDp = 22,
                startPaddingDp = 9,
                endPaddingDp = 9,
                fontSizeSp = 12,
            ),
            profileBadgeRenderMetrics(
                css = "background: linear-gradient(135deg, #22d3ee, #a855f7);",
                display = ProfileBadgeDisplay.Hero,
            ),
        )
    }

    @Test
    fun knownSourceActivityEndpointPlaceholderUsesTheFriendlyEmptyState() {
        val unavailable = NovalPieApiException(
            statusCode = 404,
            path = "/api/users/100000/activities",
            serverMessage = "API endpoint not implemented in Laravel yet"
        )

        assertTrue(sourceActivitiesEndpointUnavailable(unavailable))
        assertTrue(sourceActivitiesEndpointUnavailable(IllegalStateException("wrapped", unavailable)))
        assertTrue(sourceActivitiesEndpointUnavailable(NovalPieApiException(501, "/api/users/me/activities", "not implemented")))
        assertTrue(sourceBooksEndpointUnavailable(NovalPieApiException(404, "/api/users/100000/novels", "API endpoint not implemented in Laravel yet")))
        assertFalse(sourceBooksEndpointUnavailable(unavailable))
        assertFalse(sourceActivitiesEndpointUnavailable(NovalPieApiException(500, "/api/users/100000/activities", "not implemented")))
        assertFalse(sourceActivitiesEndpointUnavailable(NovalPieApiException(501, "/api/users/100000", "not implemented")))
        assertFalse(sourceActivitiesEndpointUnavailable(IllegalStateException("timeout")))
    }

    @Test
    fun publicProfileCollectionCountUsesTheVersionedNovelListWhenProfileOmitsStats() {
        val profile = profileWithPublicCollectionCounts(
            profile = UserProfile(id = 100002, name = "榛名全色"),
            feed = UserContentActivityFeed(),
            novels = listOf(
                com.novalpie.nativeapp.model.NovelCard(id = 361074, title = "一本书"),
                com.novalpie.nativeapp.model.NovelCard(id = 361075, title = "另一本书"),
            ),
        )

        assertEquals(2L, profile?.stats?.get("novels"))
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
