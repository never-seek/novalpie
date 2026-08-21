package com.novalpie.nativeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Native shell for all three source auth routes. CAPTCHA remains a source-owned security step. */
@Composable
fun AuthScreen(
    page: AuthPage,
    state: AuthState,
    onPageSelected: (AuthPage) -> Unit,
    onLoginMethodSelected: (AuthLoginMethod) -> Unit,
    onLoginUsernameChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onLoginEmailChange: (String) -> Unit,
    onLoginCodeChange: (String) -> Unit,
    onRegisterEmailChange: (String) -> Unit,
    onRegisterCodeChange: (String) -> Unit,
    onRegisterUsernameChange: (String) -> Unit,
    onRegisterPasswordChange: (String) -> Unit,
    onRegisterConfirmPasswordChange: (String) -> Unit,
    onResetEmailChange: (String) -> Unit,
    onResetPasswordChange: (String) -> Unit,
    onResetConfirmPasswordChange: (String) -> Unit,
    onSendLoginCode: () -> Unit,
    onSubmitLogin: () -> Unit,
    onSendRegistrationCode: () -> Unit,
    onVerifyRegistrationCode: () -> Unit,
    onSubmitRegistration: () -> Unit,
    onRequestPasswordReset: () -> Unit,
    onSubmitPasswordReset: () -> Unit,
    onOpenWebLogin: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (page) {
                            AuthPage.Login -> "欢迎回来"
                            AuthPage.Register -> "创建账号"
                            AuthPage.ResetPassword -> "重置密码"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when (page) {
                            AuthPage.Login -> "使用原生表单登录；验证码由源站安全组件完成。"
                            AuthPage.Register -> "邮箱验证后设置用户名和密码，注册成功会自动同步账号。"
                            AuthPage.ResetPassword -> if (state.resetToken.isBlank()) {
                                "输入邮箱后，源站会发送可直接回到 App 的重置链接。"
                            } else {
                                "请设置符合源站规则的新密码。"
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        when (page) {
            AuthPage.Login -> {
                item {
                    LoginMethodSwitch(
                        selected = state.loginMethod,
                        onSelected = onLoginMethodSelected
                    )
                }
                item {
                    if (state.loginMethod == AuthLoginMethod.Password) {
                        PasswordLoginFields(
                            username = state.loginUsername,
                            password = state.loginPassword,
                            onUsernameChange = onLoginUsernameChange,
                            onPasswordChange = onLoginPasswordChange
                        )
                    } else {
                        VerificationLoginFields(
                            email = state.loginEmail,
                            code = state.loginCode,
                            loading = state.actionLoading,
                            onEmailChange = onLoginEmailChange,
                            onCodeChange = onLoginCodeChange,
                            onSendCode = onSendLoginCode
                        )
                    }
                }
                item {
                    AuthCaptchaCard(state.captchaToken)
                }
                item {
                    Button(
                        onClick = onSubmitLogin,
                        enabled = !state.actionLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (state.actionLoading) "正在登录…" else "登录") }
                }
                item {
                    AuthNavigationRow(
                        leading = "没有账号？",
                        action = "去注册",
                        onAction = { onPageSelected(AuthPage.Register) }
                    )
                }
                item {
                    TextButton(onClick = { onPageSelected(AuthPage.ResetPassword) }, modifier = Modifier.fillMaxWidth()) {
                        Text("忘记密码？")
                    }
                }
            }
            AuthPage.Register -> {
                item { RegisterStepBar(state.registerStep) }
                item {
                    when (state.registerStep) {
                        AuthRegisterStep.Email -> RegisterEmailFields(
                            email = state.registerEmail,
                            loading = state.actionLoading,
                            onEmailChange = onRegisterEmailChange,
                            onSend = onSendRegistrationCode
                        )
                        AuthRegisterStep.Verify -> RegisterVerificationFields(
                            email = state.registerEmail,
                            code = state.registerCode,
                            loading = state.actionLoading,
                            onCodeChange = onRegisterCodeChange,
                            onVerify = onVerifyRegistrationCode
                        )
                        AuthRegisterStep.Account -> RegisterAccountFields(
                            username = state.registerUsername,
                            password = state.registerPassword,
                            confirmPassword = state.registerConfirmPassword,
                            loading = state.actionLoading,
                            onUsernameChange = onRegisterUsernameChange,
                            onPasswordChange = onRegisterPasswordChange,
                            onConfirmPasswordChange = onRegisterConfirmPasswordChange,
                            onSubmit = onSubmitRegistration
                        )
                    }
                }
                if (state.registerStep == AuthRegisterStep.Email) item { AuthCaptchaCard(state.captchaToken) }
                item {
                    AuthNavigationRow(
                        leading = "已有账号？",
                        action = "去登录",
                        onAction = { onPageSelected(AuthPage.Login) }
                    )
                }
            }
            AuthPage.ResetPassword -> {
                item {
                    if (state.resetToken.isBlank()) {
                        ResetRequestFields(
                            email = state.resetEmail,
                            loading = state.actionLoading,
                            onEmailChange = onResetEmailChange,
                            onRequest = onRequestPasswordReset
                        )
                    } else {
                        ResetPasswordFields(
                            password = state.resetPassword,
                            confirmPassword = state.resetConfirmPassword,
                            loading = state.actionLoading,
                            onPasswordChange = onResetPasswordChange,
                            onConfirmPasswordChange = onResetConfirmPasswordChange,
                            onSubmit = onSubmitPasswordReset
                        )
                    }
                }
                item {
                    TextButton(onClick = { onPageSelected(AuthPage.Login) }, modifier = Modifier.fillMaxWidth()) {
                        Text("返回登录")
                    }
                }
            }
        }
        state.actionMessage?.let { message -> item { AuthMessage(message) } }
        item {
            TextButton(onClick = onOpenWebLogin, modifier = Modifier.fillMaxWidth()) {
                Text("使用源站网页登录")
            }
        }
    }
}

@Composable
private fun LoginMethodSwitch(selected: AuthLoginMethod, onSelected: (AuthLoginMethod) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AuthModeButton("密码登录", selected == AuthLoginMethod.Password, Modifier.weight(1f)) {
            onSelected(AuthLoginMethod.Password)
        }
        AuthModeButton("验证码登录", selected == AuthLoginMethod.VerificationCode, Modifier.weight(1f)) {
            onSelected(AuthLoginMethod.VerificationCode)
        }
    }
}

@Composable
private fun AuthModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(label) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
}

@Composable
private fun PasswordLoginFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    AuthCard {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("用户名或邮箱") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        PasswordField(password, onPasswordChange, "您的密码")
    }
}

@Composable
private fun VerificationLoginFields(
    email: String,
    code: String,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit
) {
    AuthCard {
        OutlinedTextField(email, onEmailChange, label = { Text("请输入您的邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(code, onCodeChange, label = { Text("请输入验证码") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onSendCode, enabled = !loading, modifier = Modifier.align(Alignment.CenterVertically)) {
                Text("发送验证码")
            }
        }
    }
}

@Composable
private fun RegisterEmailFields(email: String, loading: Boolean, onEmailChange: (String) -> Unit, onSend: () -> Unit) {
    AuthCard {
        Text("邮箱验证", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("请输入您的邮箱地址，我们将发送验证码", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(email, onEmailChange, label = { Text("请输入邮箱地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSend, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("发送验证码") }
    }
}

@Composable
private fun RegisterVerificationFields(email: String, code: String, loading: Boolean, onCodeChange: (String) -> Unit, onVerify: () -> Unit) {
    AuthCard {
        Text("验证邮箱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("已向 $email 发送验证码", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(code, onCodeChange, label = { Text("请输入 6 位验证码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = onVerify, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("验证邮箱") }
    }
}

@Composable
private fun RegisterAccountFields(
    username: String,
    password: String,
    confirmPassword: String,
    loading: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    AuthCard {
        Text("完善注册信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(username, onUsernameChange, label = { Text("用户名（3-50 个字符）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        PasswordField(password, onPasswordChange, "密码（至少 6 位，含大小写字母和数字）")
        PasswordField(confirmPassword, onConfirmPasswordChange, "确认密码")
        Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("完成注册") }
    }
}

@Composable
private fun ResetRequestFields(email: String, loading: Boolean, onEmailChange: (String) -> Unit, onRequest: () -> Unit) {
    AuthCard {
        Text("申请重置链接", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(email, onEmailChange, label = { Text("请输入您的邮箱地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = onRequest, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("发送重置邮件") }
    }
}

@Composable
private fun ResetPasswordFields(
    password: String,
    confirmPassword: String,
    loading: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    AuthCard {
        Text("请输入新密码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        PasswordField(password, onPasswordChange, "新密码（至少 6 位，含大小写字母和数字）")
        PasswordField(confirmPassword, onConfirmPasswordChange, "确认新密码")
        Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("重置密码") }
    }
}

@Composable
private fun AuthCaptchaCard(token: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(authCaptchaStatusLabel(token), fontWeight = FontWeight.Bold)
            Text(
                if (token.isNullOrBlank()) "提交时会打开源站的 Turnstile / reCAPTCHA / hCaptcha 验证。"
                else "令牌仅保存在本次界面内，用于下一次源站认证请求。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RegisterStepBar(step: AuthRegisterStep) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(AuthRegisterStep.Email to "1 邮箱", AuthRegisterStep.Verify to "2 验证", AuthRegisterStep.Account to "3 信息").forEach { (item, label) ->
            Surface(
                modifier = Modifier.weight(1f),
                color = if (step.ordinal >= item.ordinal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(label, modifier = Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AuthNavigationRow(leading: String, action: String, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(leading, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun AuthMessage(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
