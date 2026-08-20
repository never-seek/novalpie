package com.novalpie.nativeapp.data

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthTokenProfileTest {
    @Test
    fun decodesCurrentWebsiteNestedJwtUserFields() {
        val token = jwt(
            """{"sub":100000,"exp":4102444800,"data":{"username":"seeking","role":"admin"}}"""
        )

        val profile = decodeAuthTokenProfile(token, nowEpochSeconds = 1_800_000_000)

        assertEquals(100000L, profile?.id)
        assertEquals("seeking", profile?.name)
        assertEquals("admin", profile?.role)
    }

    @Test
    fun rejectsExpiredOrMalformedTokens() {
        val expired = jwt(
            """{"sub":100000,"exp":100,"data":{"username":"seeking","role":"admin"}}"""
        )

        assertNull(decodeAuthTokenProfile(expired, nowEpochSeconds = 101))
        assertNull(decodeAuthTokenProfile("not-a-jwt", nowEpochSeconds = 1))
    }

    private fun jwt(payload: String): String {
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))
        return "header.$encoded.signature"
    }
}
