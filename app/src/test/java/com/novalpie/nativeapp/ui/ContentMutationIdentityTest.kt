package com.novalpie.nativeapp.ui

import org.junit.Assert.*
import org.junit.Test

class ContentMutationIdentityTest {
    @Test fun aDetailWriteOnlyReturnsToTheExactRouteSnapshotThatSubmittedIt() {
        val route=AppRoute.ForumPostDetail(42)
        assertTrue(isCurrentContentMutation(ContentMutationIdentity(route,3,7),route,3,7))
        assertFalse(isCurrentContentMutation(ContentMutationIdentity(route,3,7),AppRoute.ForumPostDetail(43),3,7))
        assertFalse(isCurrentContentMutation(ContentMutationIdentity(route,3,7),route,4,7))
        assertFalse(isCurrentContentMutation(ContentMutationIdentity(route,3,7),route,3,8))
        assertFalse(isCurrentContentMutation(ContentMutationIdentity(AppRoute.BookDetail(42),3,7),route,3,7))
    }
}
