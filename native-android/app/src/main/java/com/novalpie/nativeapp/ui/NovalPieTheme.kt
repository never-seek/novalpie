package com.novalpie.nativeapp.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.novalpie.nativeapp.ui.design.NovalPieDarkColorTokens
import com.novalpie.nativeapp.ui.design.NovalPieLightColorTokens
import com.novalpie.nativeapp.ui.design.NovalPieShapes
import com.novalpie.nativeapp.ui.design.NovalPieTypography
import com.novalpie.nativeapp.ui.design.toColorScheme

/**
 * Application theme.
 *
 * Previously this file was the entire design system: 14 colour roles and a shape set, with
 * `typography` never passed to [MaterialTheme] at all. The tokens now live in `ui/design/`, and
 * this file's remaining job is to assemble them and reconcile the Compose theme with the
 * platform window.
 *
 * That reconciliation was the visible defect. `styles.xml` hardcoded warm `#FFF8F4` status and
 * navigation bars, matching neither the light page (`#F2F2F2`) nor the top bar (`#FFFFFF`), which
 * produced a three-band seam at the top of every screen; in dark mode those same cream bars sat
 * above a `#191C1F` app, still with dark icons. Nothing switched them, because there was no
 * `values-night`, no `WindowCompat` call and no edge-to-edge setup anywhere in the app. The bars
 * are now transparent, and their icon tint follows the active theme.
 */
@Composable
fun NovalPieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme) {
        if (darkTheme) {
            NovalPieDarkColorTokens.toColorScheme()
        } else {
            NovalPieLightColorTokens.toColorScheme()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            // Light theme -> dark icons on a transparent bar, and the reverse in dark mode.
            // This is what was missing: the bars kept dark icons in both themes.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NovalPieTypography,
        shapes = NovalPieShapes,
        content = content,
    )
}
