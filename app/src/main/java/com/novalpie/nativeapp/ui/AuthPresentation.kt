package com.novalpie.nativeapp.ui

/** Matches the three public source routes rather than treating authentication as one web fallback. */
enum class AuthPage(val websitePath: String, val title: String) {
    Login("/login", "登录"),
    Register("/register", "创建账号"),
    ResetPassword("/reset-password", "重置密码")
}

enum class AuthLoginMethod {
    Password,
    VerificationCode
}

enum class AuthRegisterStep {
    Email,
    Verify,
    Account
}

/** A CAPTCHA token is transient and used only for the pending source request. */
enum class AuthCaptchaAction {
    PasswordLogin,
    SendLoginCode,
    LoginWithCode,
    SendRegistrationCode
}

internal fun validateAuthEmail(value: String): String? = when {
    value.trim().isBlank() -> "请输入邮箱地址"
    !EMAIL_PATTERN.matches(value.trim()) -> "邮箱格式不正确"
    else -> null
}

internal fun validateAuthCode(value: String): String? = when {
    value.isBlank() -> "请输入验证码"
    !CODE_PATTERN.matches(value.trim()) -> "验证码必须是 6 位数字"
    else -> null
}

internal fun validateAuthUsername(value: String): String? = when {
    value.trim().isBlank() -> "请输入用户名"
    value.trim().length !in 3..50 -> "用户名长度必须在 3-50 个字符之间"
    else -> null
}

internal fun validateAuthPassword(value: String): String? = when {
    value.isBlank() -> "请输入密码"
    value.length < 6 ||
        value.none(Char::isUpperCase) ||
        value.none(Char::isLowerCase) ||
        value.none(Char::isDigit) -> "密码必须至少 6 位，包含大小写字母和数字"
    else -> null
}

internal fun authCaptchaStatusLabel(token: String?): String =
    if (token.isNullOrBlank()) "需要安全验证" else "安全验证已完成"

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val CODE_PATTERN = Regex("^\\d{6}$")
