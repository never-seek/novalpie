package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebsiteDeepLinkRouteTest {

    @Test
    fun mapsPublicWebsiteRoutesToNativeDestinations() {
        assertEquals(AppRoute.Home, nativeWebsiteRoute("/", isAdmin = false))
        assertEquals(AppRoute.Home, nativeWebsiteRoute("/favorites", isAdmin = false))
        assertEquals(AppRoute.Search, nativeWebsiteRoute("/search", isAdmin = false))
        assertEquals(AppRoute.Forum, nativeWebsiteRoute("/forum", isAdmin = false))
        assertEquals(AppRoute.ForumPostDetail(1422), nativeWebsiteRoute("/forum/1422", isAdmin = false))
        assertEquals(AppRoute.ForumPostDetail(1422), nativeWebsiteRoute("/posts/1422", isAdmin = false))
        assertEquals(AppRoute.ForumCreate, nativeWebsiteRoute("/forum/create", isAdmin = false))
        assertEquals(AppRoute.Profile, nativeWebsiteRoute("/user", isAdmin = false))
        assertEquals(AppRoute.UserProfileDetail(100000), nativeWebsiteRoute("/user/100000", isAdmin = false))
        assertEquals(AppRoute.MessageCenter, nativeWebsiteRoute("/messages", isAdmin = false))
        assertEquals(AppRoute.Workspace, nativeWebsiteRoute("/workspace", isAdmin = false))
        assertEquals(AppRoute.UploadBook, nativeWebsiteRoute("/upload", isAdmin = false))
        assertEquals(AppRoute.UploadEditor, nativeWebsiteRoute("/upload-editor", isAdmin = false))
        assertEquals(AppRoute.PoliticalExam, nativeWebsiteRoute("/political-exam", isAdmin = false))
        assertEquals(AppRoute.Home, nativeWebsiteRoute("/reader", isAdmin = false))
        assertEquals(AppRoute.Auth(AuthPage.Login), nativeWebsiteRoute("/login", isAdmin = false))
        assertEquals(AppRoute.Auth(AuthPage.Register), nativeWebsiteRoute("/register", isAdmin = false))
        assertEquals(AppRoute.Auth(AuthPage.ResetPassword), nativeWebsiteRoute("/reset-password", isAdmin = false))
    }

    @Test
    fun mapsBookReaderAndManagedBookRoutesWithoutTreatingIdsAsOptional() {
        assertEquals(AppRoute.BookDetail(354491), nativeWebsiteRoute("/book-detail/354491", isAdmin = false))
        assertEquals(AppRoute.BookDetail(354491), nativeWebsiteRoute("/book/354491", isAdmin = false))
        assertEquals(AppRoute.Reader(354491, 6992449), nativeWebsiteRoute("/book/354491/6992449", isAdmin = false))
        assertEquals(AppRoute.BookEditInfo(354491), nativeWebsiteRoute("/book-edit/info/354491", isAdmin = false))
        assertEquals(AppRoute.BookChapters(354491), nativeWebsiteRoute("/book-edit/chapters/354491", isAdmin = false))
        assertEquals(AppRoute.BookAppend(354491), nativeWebsiteRoute("/book-edit/append/354491", isAdmin = false))
        assertNull(nativeWebsiteRoute("/book/not-a-number", isAdmin = false))
        assertNull(nativeWebsiteRoute("/book/354491/not-a-number", isAdmin = false))
    }

    @Test
    fun mapsReaderLandingQueryExactlyLikeTheLiveWebsiteRedirect() {
        assertEquals(AppRoute.Reader(354491, 6992449), readerLandingRoute("354491", "6992449"))
        assertEquals(AppRoute.Home, readerLandingRoute("354491", null))
        assertEquals(AppRoute.Home, readerLandingRoute("not-a-number", "6992449"))
        assertEquals(AppRoute.Home, readerLandingRoute("0", "0"))
    }

    @Test
    fun restrictsAdministratorRoutesBeforeTheyReachNavigation() {
        assertNull(nativeWebsiteRoute("/admin", isAdmin = false))
        assertNull(nativeWebsiteRoute("/admin/review", isAdmin = false))
        assertEquals(AppRoute.Admin(AdminSection.Overview), nativeWebsiteRoute("/admin", isAdmin = true))
        assertEquals(AppRoute.Admin(AdminSection.Review), nativeWebsiteRoute("/admin/review", isAdmin = true))
        assertEquals(AppRoute.Admin(AdminSection.Keys), nativeWebsiteRoute("/admin/key-management", isAdmin = true))
        assertEquals(AppRoute.Admin(AdminSection.OperationLogs), nativeWebsiteRoute("/admin/operation-logs", isAdmin = true))
        assertEquals(AppRoute.Admin(AdminSection.Scraper), nativeWebsiteRoute("/admin/scraper-management", isAdmin = true))
        assertEquals(AppRoute.Admin(AdminSection.Shop), nativeWebsiteRoute("/admin/shop", isAdmin = true))
    }

    @Test
    fun leavesUnknownPagesToTheirExplicitFallback() {
        assertNull(nativeWebsiteRoute("/admin/not-real", isAdmin = true))
        assertNull(nativeWebsiteRoute("/forum/create/not-real", isAdmin = false))
        assertNull(nativeWebsiteRoute("/workspace/not-real", isAdmin = false))
        assertNull(nativeWebsiteRoute("/unknown", isAdmin = false))
    }
}
