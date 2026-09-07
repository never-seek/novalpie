package com.novalpie.nativeapp.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Notification permission is optional for FGS work, but required for visible background controls. */
@Composable
internal fun rememberBackgroundTaskAction(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var deniedThisVisit by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        deniedThisVisit = !granted
        if (!granted) Toast.makeText(context, "通知未开启，后台任务仍会运行；暂停、继续请回到 App 操作", Toast.LENGTH_LONG).show()
        val action = pending; pending = null
        action?.invoke()
    }
    return { action ->
        if (Build.VERSION.SDK_INT < 33 || deniedThisVisit ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) action()
        else if (pending == null) {
            pending = action
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
