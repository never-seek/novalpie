package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ForumPost
import com.novalpie.nativeapp.model.ForumPostDetail
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserContentActivityFeed
import com.novalpie.nativeapp.model.UserProfile
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLoadPresentationTest {
    @Test
    fun forumDetailArrivingFirstLeavesSlowCommentsInLoadingState() {
        val detail = ForumPostDetail(
            post = ForumPost(id = 1828, category = "discussion", title = "即时显示正文"),
            content = "正文已经返回",
        )

        val updated = forumPostDetailWithLoadedDetail(
            ForumPostDetailState(
                postId = 1828,
                detail = LoadResult.Loading,
                comments = LoadResult.Loading,
            ),
            Result.success(detail),
        )

        assertEquals(detail, (updated.detail as LoadResult.Success<ForumPostDetail>).value)
        assertTrue(updated.comments is LoadResult.Loading)
    }

    @Test
    fun forumCommentsFailureDoesNotHideAnAlreadyLoadedPost() {
        val detail = ForumPostDetail(
            post = ForumPost(id = 1828, category = "discussion", title = "帖子仍可阅读"),
        )

        val updated = forumPostDetailWithLoadedComments(
            ForumPostDetailState(
                postId = 1828,
                detail = LoadResult.Success(detail),
                comments = LoadResult.Loading,
            ),
            Result.failure<List<com.novalpie.nativeapp.model.ForumComment>>(IOException("offline")),
            retainVisibleComments = false,
        )

        assertEquals(detail, (updated.detail as LoadResult.Success<ForumPostDetail>).value)
        assertTrue(updated.comments is LoadResult.Error)
    }

    @Test
    fun publicProfileArrivingFirstLeavesIndependentPanelsLoading() {
        val profile = UserProfile(id = 100000, name = "诺亚方舟")

        val updated = userProfileDetailWithLoadedProfile(
            UserProfileDetailState(
                userId = 100000,
                profile = LoadResult.Loading,
                activities = LoadResult.Loading,
                books = LoadResult.Loading,
            ),
            Result.success(profile),
        )

        assertEquals(profile, (updated.profile as LoadResult.Success<UserProfile>).value)
        assertTrue(updated.activities is LoadResult.Loading)
        assertTrue(updated.books is LoadResult.Loading)
    }

    @Test
    fun publicActivityFailureDoesNotHideAnAlreadyLoadedProfile() {
        val profile = UserProfile(id = 100000, name = "诺亚方舟")
        val updated = userProfileDetailWithLoadedActivities(
            UserProfileDetailState(
                userId = 100000,
                profile = LoadResult.Success(profile),
                activities = LoadResult.Loading,
            ),
            Result.failure<UserContentActivityFeed>(IOException("offline")),
        )

        assertEquals(profile, (updated.profile as LoadResult.Success<UserProfile>).value)
        assertTrue(updated.activities is LoadResult.Error)
    }

    @Test
    fun publicProfilePanelRailRemainsAvailableWhenHeroFailsButActivitiesArrive() {
        val state = UserProfileDetailState(
            userId = 100000,
            profile = LoadResult.Error("用户资料暂时无法加载"),
            activities = LoadResult.Success(
                listOf(UserActivity(id = 1, type = "post", title = "动态仍可阅读")),
            ),
        )

        assertTrue(shouldRenderPublicProfilePanels(state))
    }

    @Test
    fun publicActivityFeedCanUpdateProfileCountersAfterProfileIsVisible() {
        val profile = UserProfile(id = 100000, name = "诺亚方舟")
        val feed = UserContentActivityFeed(
            activities = listOf(UserActivity(id = 1, type = "post", title = "动态")),
            postCount = 4,
            forumCommentCount = 7,
            bookReviewCount = 3,
        )

        val updated = userProfileDetailWithLoadedActivities(
            UserProfileDetailState(
                userId = 100000,
                profile = LoadResult.Success(profile),
                activities = LoadResult.Loading,
            ),
            Result.success(feed),
        )

        assertEquals(4L, (updated.profile as LoadResult.Success<UserProfile>).value.stats["posts"])
        assertEquals(10L, (updated.profile as LoadResult.Success<UserProfile>).value.stats["comments"])
        assertEquals(feed.activities, (updated.activities as LoadResult.Success<List<UserActivity>>).value)
    }

    @Test
    fun publicProfileMergesActivityCountersWhenTheProfileArrivesLast() {
        val feed = UserContentActivityFeed(
            activities = listOf(UserActivity(id = 1, type = "post", title = "动态")),
            postCount = 4,
            forumCommentCount = 7,
            bookReviewCount = 3,
        )
        val withFeed = userProfileDetailWithLoadedActivities(
            UserProfileDetailState(userId = 100000, profile = LoadResult.Loading),
            Result.success(feed),
        )

        val updated = userProfileDetailWithLoadedProfile(
            withFeed,
            Result.success(UserProfile(id = 100000, name = "诺亚方舟")),
        )

        assertEquals(4L, (updated.profile as LoadResult.Success<UserProfile>).value.stats["posts"])
        assertEquals(10L, (updated.profile as LoadResult.Success<UserProfile>).value.stats["comments"])
    }

    @Test
    fun staleForumBookReferenceResolutionDoesNotEraseTheNewerReferenceSet() {
        val newest = ForumPostDetailState(
            postId = 1828,
            bookReferences = mapOf(354491L to LoadResult.Loading),
        )

        val updated = forumPostDetailWithResolvedBookReferences(
            state = newest,
            resolutionSerial = 11,
            activeResolutionSerial = 12,
            resolved = mapOf(350192L to LoadResult.Success(NovelCard(350192, "旧请求的书"))),
        )

        assertEquals(newest.bookReferences, updated.bookReferences)
    }

    @Test
    fun retryingForumCommentsKeepsTheLoadedPostVisible() {
        val detail = ForumPostDetail(
            post = ForumPost(id = 1828, category = "discussion", title = "正文不能被重试遮住"),
            content = "正文",
        )

        val updated = forumPostDetailForCommentsRetry(
            ForumPostDetailState(
                postId = 1828,
                detail = LoadResult.Success(detail),
                comments = LoadResult.Error("网络失败"),
            ),
        )

        assertEquals(detail, (updated.detail as LoadResult.Success<ForumPostDetail>).value)
        assertTrue(updated.comments is LoadResult.Loading)
    }

    @Test
    fun retryingForumPostKeepsTheLoadedCommentsVisible() {
        val comments = listOf(com.novalpie.nativeapp.model.ForumComment(id = 91, content = "已加载评论"))

        val updated = forumPostDetailForPostRetry(
            ForumPostDetailState(
                postId = 1828,
                detail = LoadResult.Error("网络失败"),
                comments = LoadResult.Success(comments),
            ),
        )

        assertTrue(updated.detail is LoadResult.Loading)
        assertEquals(comments, (updated.comments as LoadResult.Success<List<com.novalpie.nativeapp.model.ForumComment>>).value)
    }

    @Test
    fun retryingPublicActivitiesKeepsTheProfileHeroVisible() {
        val profile = UserProfile(id = 100000, name = "诺亚方舟")

        val updated = userProfileDetailForActivitiesRetry(
            UserProfileDetailState(
                userId = 100000,
                profile = LoadResult.Success(profile),
                activities = LoadResult.Error("网络失败"),
            ),
        )

        assertEquals(profile, (updated.profile as LoadResult.Success<UserProfile>).value)
        assertTrue(updated.activities is LoadResult.Loading)
    }

    @Test
    fun retryingOnePublicProfilePanelLeavesOtherSuccessfulPanelsUntouched() {
        val profile = UserProfile(id = 100000, name = "诺亚方舟")
        val activities = listOf(UserActivity(id = 1, type = "post", title = "动态"))

        val updated = userProfileDetailForPanelRetry(
            state = UserProfileDetailState(
                userId = 100000,
                profile = LoadResult.Success(profile),
                activities = LoadResult.Success(activities),
                books = LoadResult.Error("网络失败"),
            ),
            panel = PublicUserProfilePanel.Books,
        )

        assertEquals(profile, (updated.profile as LoadResult.Success<UserProfile>).value)
        assertEquals(activities, (updated.activities as LoadResult.Success<List<UserActivity>>).value)
        assertTrue(updated.books is LoadResult.Loading)
    }
}
