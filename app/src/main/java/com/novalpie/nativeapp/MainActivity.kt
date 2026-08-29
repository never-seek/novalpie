package com.novalpie.nativeapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
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
