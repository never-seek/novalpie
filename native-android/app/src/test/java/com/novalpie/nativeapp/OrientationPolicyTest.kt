package com.novalpie.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationPolicyTest {
    @Test
    fun handsetAndEmulatorWidthsStayPortraitFirst() {
        assertTrue(novalPieShouldLockPortrait(0))
        assertTrue(novalPieShouldLockPortrait(411))
        assertTrue(novalPieShouldLockPortrait(599))
        assertTrue(novalPieShouldLockPortrait(600))
    }

    @Test
    fun tabletWidthsCanFollowSystemRotation() {
        assertFalse(novalPieShouldLockPortrait(601))
        assertFalse(novalPieShouldLockPortrait(840))
    }
}
