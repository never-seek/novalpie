package com.novalpie.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainActivityStartUriTest {

    @Test
    fun coldActionViewIntentIsDispatchedEvenWhenAndroidRestoresTaskState() {
        val forumUri = "novalpie://app/forum/1828"

        assertEquals(
            forumUri,
            initialActivityStartUri(
                action = "android.intent.action.VIEW",
                dataUri = forumUri,
                restoredHandledUri = null,
            ),
        )
    }

    @Test
    fun rotationDoesNotDispatchTheSameAlreadyHandledDeepLinkAgain() {
        val forumUri = "novalpie://app/forum/1828"

        assertNull(
            initialActivityStartUri(
                action = "android.intent.action.VIEW",
                dataUri = forumUri,
                restoredHandledUri = forumUri,
            ),
        )
    }
}
