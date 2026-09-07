package com.novalpie.nativeapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.core.rememberBackgroundTaskAction
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Run after explicitly restoring the pre-test notification permission to denied. No task executes. */
class BackgroundTaskPermissionDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    @Test fun firstBackgroundActionRequestsNotificationsAndRunsExactlyOnceAfterGrant() {
        val context = compose.activity
        val calls = AtomicInteger()
        val needsPermission = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        compose.setContent { MaterialTheme {
            val start = rememberBackgroundTaskAction()
            Button(onClick = { start { calls.incrementAndGet() } }) { Text("启动合成任务") }
        } }
        compose.onNodeWithText("启动合成任务").performClick()
        if (needsPermission) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.serviceInfo = automation.serviceInfo.apply {
                flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            }
            fun allowNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                node ?: return null
                if (node.packageName?.toString()?.endsWith("permissioncontroller") == true &&
                    node.viewIdResourceName?.endsWith(":id/permission_allow_button") == true) return node
                for (i in 0 until node.childCount) allowNode(node.getChild(i))?.let { return it }
                return null
            }
            compose.waitUntil(10000) { allowNode(automation.rootInActiveWindow) != null }
            assertEquals(0, calls.get())
            assertTrue(allowNode(automation.rootInActiveWindow)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        compose.waitUntil(5000) { calls.get() == 1 }
        compose.onNodeWithText("启动合成任务").performClick()
        compose.waitUntil(5000) { calls.get() == 2 }
        if (Build.VERSION.SDK_INT >= 33) assertEquals(PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS))
    }
}
