package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPresentationTest {
    @Test
    fun sourceAuthValidationRulesAreEnforcedBeforeARequestIsMade() {
        assertEquals("邮箱格式不正确", validateAuthEmail("not-an-email"))
        assertNull(validateAuthEmail("reader@example.com"))
        assertEquals("验证码必须是 6 位数字", validateAuthCode("12345"))
        assertNull(validateAuthCode("123456"))
        assertEquals("用户名长度必须在 3-50 个字符之间", validateAuthUsername("ab"))
        assertNull(validateAuthUsername("native-reader"))
        assertEquals("密码必须至少 6 位，包含大小写字母和数字", validateAuthPassword("lowercase1"))
        assertNull(validateAuthPassword("PassWord1"))
    }

    @Test
    fun authPagesAndCaptchaStatusMatchTheCurrentWebsiteFlow() {
        assertEquals("/login", AuthPage.Login.websitePath)
        assertEquals("/register", AuthPage.Register.websitePath)
        assertEquals("/reset-password", AuthPage.ResetPassword.websitePath)
        assertEquals("需要安全验证", authCaptchaStatusLabel(null))
        assertEquals("安全验证已完成", authCaptchaStatusLabel("temporary-token"))
    }

    @Test
    fun captchaProxyLoadUsesTheSameRouteMarkerAsTheSharedWebViewLoader() {
        val stateKey = "auto: 127.0.0.1:7890"
        val marker = captchaWebViewStateMarker(stateKey)
        assertEquals(stateKey, marker.stateKey)
        assertEquals("https://novalpie.cc/login", marker.requestedUrl)
        assertTrue(webViewMatchesRequest(marker, stateKey, marker.requestedUrl))
    }
}
