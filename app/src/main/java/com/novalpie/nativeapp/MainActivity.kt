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

class MainActivity : ComponentActivity() {

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

        // savedInstanceState != null means this is a configuration change, most often a rotation.
        // Re-reading intent.data there re-ran the deep link and threw the reader back to the
        // originally linked chapter, discarding wherever the user had navigated to since.
        if (savedInstanceState == null) {
            startUri = intent?.data?.toString()
        }

        setContent {
            NovalPieApp(
                startUri = startUri,
                onStartUriHandled = { startUri = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { startUri = it }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = readerVolumeKeyHandler
        return when (
            readerVolumeKeyAction(
                keyCode = event.keyCode,
                action = event.action,
                repeatCount = event.repeatCount,
                pageTurnEnabled = handler != null,
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
            ReaderVolumeKeyAction.Ignore -> super.dispatchKeyEvent(event)
        }
    }

}
