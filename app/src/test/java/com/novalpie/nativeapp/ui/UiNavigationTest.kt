package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.LoadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class UiNavigationTest {
    @Test
    fun bottomTabsMatchForumReaderProductStructure() {
        assertEquals(
            listOf(BottomTab.Collection, BottomTab.Discover, BottomTab.Tools, BottomTab.Forum, BottomTab.Profile),
            BottomTab.values().toList()
        )
    }

    @Test
    fun bottomTabEnumTitlesAreCleanProductLabels() {
        assertEquals(listOf("收藏", "搜索", "工具", "论坛", "我的"), BottomTab.values().map { it.title })
    }

    @Test
    fun bottomTabLabelsAreCleanChineseProductLabels() {
        assertEquals("收藏", bottomTabDisplayLabel(BottomTab.Collection))
        assertEquals("搜索", bottomTabDisplayLabel(BottomTab.Discover))
        assertEquals("工具", bottomTabDisplayLabel(BottomTab.Tools))
        assertEquals("论坛", bottomTabDisplayLabel(BottomTab.Forum))
        assertEquals("我的", bottomTabDisplayLabel(BottomTab.Profile))
    }

    @Test
    fun bottomTabShortLabelsAreSingleCleanChineseCharacters() {
        assertEquals("收", bottomTabShortLabel(BottomTab.Collection))
        assertEquals("搜", bottomTabShortLabel(BottomTab.Discover))
        assertEquals("工", bottomTabShortLabel(BottomTab.Tools))
        assertEquals("论", bottomTabShortLabel(BottomTab.Forum))
        assertEquals("我", bottomTabShortLabel(BottomTab.Profile))
    }

    @Test
    fun rootRoutesRestoreTheirMatchingBottomTabAfterBackNavigation() {
        assertEquals(BottomTab.Collection, rootRouteTab(AppRoute.Home))
        assertEquals(BottomTab.Discover, rootRouteTab(AppRoute.Search))
        assertEquals(BottomTab.Tools, rootRouteTab(AppRoute.Tools))
        assertEquals(BottomTab.Forum, rootRouteTab(AppRoute.Forum))
        assertEquals(BottomTab.Profile, rootRouteTab(AppRoute.Profile))
        assertEquals(null, rootRouteTab(AppRoute.BookDetail(354491)))
    }

    @Test
    fun losingAdministratorAccessDropsTheAdminStackToTheToolsRoot() {
        val adminStack = listOf(
            AppRoute.Tools,
            AppRoute.Admin(AdminSection.Review),
        )

        assertEquals(
            listOf(AppRoute.Tools),
            sanitizeAdminRouteStack(adminStack, isAdmin = false),
        )
        assertEquals(
            adminStack,
            sanitizeAdminRouteStack(adminStack, isAdmin = true),
        )
        assertEquals(
            listOf(AppRoute.Forum),
            sanitizeAdminRouteStack(listOf(AppRoute.Forum), isAdmin = false),
        )
    }

    @Test
    fun authoritativeProfileRefreshRevokesAnExistingAdminRouteForAnOrdinaryAccount() {
        val adminStack = listOf(
            AppRoute.Tools,
            AppRoute.Admin(AdminSection.OperationLogs),
        )

        assertEquals(
            listOf(AppRoute.Tools),
            sanitizeAdminRouteStackForAuthoritativeProfile(
                routes = adminStack,
                profile = UserProfile(id = 42, name = "ordinary", role = "user"),
            ),
        )
        assertEquals(
            adminStack,
            sanitizeAdminRouteStackForAuthoritativeProfile(
                routes = adminStack,
                profile = UserProfile(id = 100000, name = "administrator", role = "admin"),
            ),
        )
    }

    @Test
    fun toolsPermissionUsesCurrentProfileBeforeStaleShelfProfile() {
        val shelfProfile = LoadResult.Success(
            UserProfile(id = 100164, name = "seeking", role = "user")
        )
        val currentProfile = LoadResult.Success(
            UserProfile(id = 100164, name = "seeking", role = "admin")
        )

        assertSame(
            currentProfile.value,
            effectiveToolsUserProfile(
                profile = currentProfile,
                shelfUser = shelfProfile,
                tokenProfile = currentProfile.value,
            ),
        )
        assertNull(
            effectiveToolsUserProfile(
                profile = shelfProfile,
                shelfUser = shelfProfile,
                tokenProfile = currentProfile.value,
            )?.takeIf { isAdminProfile(it) },
        )
    }

    @Test
    fun toolsPermissionRejectsProfileFromAPreviousAccount() {
        val staleAdmin = LoadResult.Success(
            UserProfile(id = 100164, name = "old-admin", role = "admin")
        )
        val currentUser = LoadResult.Success(
            UserProfile(id = 100002, name = "ordinary", role = "user")
        )

        assertEquals(
            currentUser.value,
            effectiveToolsUserProfile(
                profile = staleAdmin,
                shelfUser = currentUser,
                tokenProfile = currentUser.value,
            ),
        )
    }

    @Test
    fun messageRoutesUseSpecificProductContextLabels() {
        assertEquals("消息中心", routeContextLabel(AppRoute.MessageCenter, BottomTab.Tools))
        assertEquals("消息详情", routeContextLabel(AppRoute.MessageDetail(7), BottomTab.Tools))
        assertEquals("私信", routeContextLabel(AppRoute.MessageConversation(20, "Alice"), BottomTab.Tools))
        assertEquals("消息设置", routeContextLabel(AppRoute.MessageSettings, BottomTab.Tools))
        assertEquals("工作区", routeContextLabel(AppRoute.Workspace, BottomTab.Tools))
        assertEquals("上传书籍", routeContextLabel(AppRoute.UploadBook, BottomTab.Tools))
        assertEquals("EPUB 编辑器", routeContextLabel(AppRoute.UploadEditor, BottomTab.Tools))
        assertEquals("收藏", routeContextLabel(AppRoute.Home, BottomTab.Collection))
        assertEquals("帖子详情", routeContextLabel(AppRoute.ForumPostDetail(7), BottomTab.Forum))
        assertEquals("书籍详情", routeContextLabel(AppRoute.BookDetail(354491), BottomTab.Discover))
        assertEquals("术语表", routeContextLabel(AppRoute.Terminology(354491), BottomTab.Discover))
        assertEquals("阅读", routeContextLabel(AppRoute.Reader(354491, 8001), BottomTab.Collection))
        assertEquals("考试", routeContextLabel(AppRoute.PoliticalExam, BottomTab.Tools))
        assertEquals("登录", routeContextLabel(AppRoute.Auth(AuthPage.Login), BottomTab.Profile))
        assertEquals("安全验证", routeContextLabel(AppRoute.AuthCaptcha, BottomTab.Profile))
    }
}
