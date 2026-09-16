package com.novalpie.nativeapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.novalpie.nativeapp.data.NetworkConfigStore
import com.novalpie.nativeapp.data.configureNovalPieImageLoader
import com.novalpie.nativeapp.ui.NovalPieApp
import com.novalpie.nativeapp.ui.ReaderVolumeKeyAction
import com.novalpie.nativeapp.ui.readerPreferredRefreshRate
import com.novalpie.nativeapp.ui.readerVolumeKeyAction

/**
 * Android may recreate a task with a non-null state bundle while delivering a new ACTION_VIEW
 * intent. Keep that incoming link, but do not replay the exact link already consumed before a
 * configuration recreation.
 */
internal fun initialActivityStartUri(
    action: String?,
    dataUri: String?,
    restoredHandledUri: String?,
): String? = dataUri?.takeIf {
    action == Intent.ACTION_VIEW && it.isNotBlank() && it != restoredHandledUri
}

class MainActivity : ComponentActivity() {

    private companion object {
        const val STATE_HANDLED_START_URI = "novalpie.handled_start_uri"
    }

    private var readerVolumeKeyHandler: ((Int) -> Unit)? = null
    private var readerOriginalPreferredRefreshRate: Float? = null
    private var readerOriginalPreferredDisplayModeId: Int? = null

    /**
     * Requests the highest rate the active physical display advertises while the immersive reader
     * is on screen. Supports both legacy preferredRefreshRate and modern preferredDisplayModeId /
     * setFrameRate so 90/120/144Hz displays across diverse OEM frameworks switch to full frame rate.
     * Leaving the reader restores the window's prior system preference.
     */
    @Suppress("DEPRECATION")
    internal fun setReaderHighRefreshRateEnabled(enabled: Boolean) {
        val params = window.attributes
        if (!enabled) {
            var modified = false
            readerOriginalPreferredRefreshRate?.let { originalRate ->
                if (params.preferredRefreshRate != originalRate) {
                    params.preferredRefreshRate = originalRate
                    modified = true
                }
            }
            readerOriginalPreferredDisplayModeId?.let { originalModeId ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && params.preferredDisplayModeId != originalModeId) {
                    params.preferredDisplayModeId = originalModeId
                    modified = true
                }
            }
            if (modified) {
                window.attributes = params
            }
            readerOriginalPreferredRefreshRate = null
            readerOriginalPreferredDisplayModeId = null
            return
        }

        val display = window.decorView.display
        val currentMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) display?.mode else null
        val supportedModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display?.supportedModes.orEmpty()
        } else {
            emptyArray()
        }

        val candidateModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentMode != null) {
            val matching = supportedModes.filter {
                it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
            }
            if (matching.isNotEmpty()) matching else supportedModes.toList()
        } else {
            supportedModes.toList()
        }

        val bestMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            candidateModes.maxByOrNull { it.refreshRate }
        } else null

        val targetRate = bestMode?.refreshRate ?: readerPreferredRefreshRate(
            supportedModes.map { it.refreshRate }
        )
        if (targetRate <= 0f) return

        if (readerOriginalPreferredRefreshRate == null) {
            readerOriginalPreferredRefreshRate = params.preferredRefreshRate
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && readerOriginalPreferredDisplayModeId == null) {
            readerOriginalPreferredDisplayModeId = params.preferredDisplayModeId
        }

        var modified = false
        if (bestMode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (params.preferredDisplayModeId != bestMode.modeId) {
                params.preferredDisplayModeId = bestMode.modeId
                modified = true
            }
        }
        if (params.preferredRefreshRate != targetRate) {
            params.preferredRefreshRate = targetRate
            modified = true
        }
        if (modified) {
            window.attributes = params
        }
    }

    /** Installs a short-lived page-turn callback owned by the currently composed reader route. */
    internal fun setReaderVolumeKeyHandler(handler: ((Int) -> Unit)?) {
        readerVolumeKeyHandler = handler
    }

    /**
     * Held in state rather than read once into a local, so that [onNewIntent] can deliver a
     * second deep link. Previously the start URI was captured only in [onCreate]; because this
     * activity is a singleTask-like launcher entry, a deep link arriving while the app was already
     * running went nowhere.
     */
    private var startUri by mutableStateOf<String?>(null)
    private var handledStartUri: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        // MuMu and handset-sized windows should not unexpectedly follow a stale landscape
        // rotation. Tablets (smallest width >= 600dp) remain free to use landscape layouts.
        if (novalPieShouldLockPortrait(resources.configuration.smallestScreenWidthDp)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        // Must precede super.onCreate so the splash theme is swapped for the app theme before the
        // first frame. The app previously had no splash at all.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Draws behind the system bars. NovalPieTheme keeps the bar icon tint in step with the
        // active colour scheme; screens consume the insets themselves.
        enableEdgeToEdge()

        configureNovalPieImageLoader(this, NetworkConfigStore(this).loadProxySettings())

        handledStartUri = savedInstanceState?.getString(STATE_HANDLED_START_URI)
        startUri = initialActivityStartUri(
            action = intent?.action,
            dataUri = intent?.data?.toString(),
            restoredHandledUri = handledStartUri,
        )

        setContent {
            NovalPieApp(
                startUri = startUri,
                onStartUriHandled = { handledUri ->
                    handledStartUri = handledUri
                    if (startUri == handledUri) startUri = null
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()
            ?.takeIf(String::isNotBlank)
            ?.let { startUri = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_HANDLED_START_URI, handledStartUri)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return handleReaderVolumeKey(event) ?: super.dispatchKeyEvent(event)
    }

    /**
     * Some MuMu firmware routes injected hardware keys through Activity.onKeyDown after the
     * window callback instead of delivering the complete sequence to dispatchKeyEvent. Keep the
     * same small decision function in both entry points so physical and adb-injected volume keys
     * have identical reader behavior. A handled dispatch never reaches onKeyDown, so a normal
     * event cannot turn twice.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return handleReaderVolumeKey(event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return handleReaderVolumeKey(event) ?: super.onKeyUp(keyCode, event)
    }

    private fun handleReaderVolumeKey(event: KeyEvent): Boolean? {
        val handler = readerVolumeKeyHandler
        return when (
            readerVolumeKeyAction(
                keyCode = event.keyCode,
                action = event.action,
                repeatCount = event.repeatCount,
                readerActive = handler != null,
            )
        ) {
            ReaderVolumeKeyAction.PreviousPage -> {
                handler?.invoke(-1)
                true
            }
            ReaderVolumeKeyAction.NextPage -> {
                handler?.invoke(1)
                true
            }
            ReaderVolumeKeyAction.Consume -> true
            ReaderVolumeKeyAction.Ignore -> null
        }
    }

}
