package com.novalpie.nativeapp.audit

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.ui.AuthCaptchaScreen
import com.novalpie.nativeapp.ui.NovalPieTheme

/** Debug-only source verification probe. Does not submit a login or store/export a CAPTCHA token. */
class CaptchaAuditActivity : ComponentActivity() {
    var verified by mutableStateOf(false)
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        val container = AppContainer.from(this)
        setContent { NovalPieTheme {
            if (verified) Column(Modifier.fillMaxSize()) {
                Text("安全验证已完成")
                Text("仅验证短期结果已回填；没有提交登录或更改账号。")
                Button(onClick = ::finish) { Text("返回") }
            } else AuthCaptchaScreen(container.environment.proxy, onToken = { verified = true }, onCancel = ::finish)
        } }
    }
    override fun onDestroy() { WebView.setWebContentsDebuggingEnabled(false); super.onDestroy() }
}
