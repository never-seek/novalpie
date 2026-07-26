package com.novalpie.nativeapp.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimension, shape and motion tokens.
 *
 * The audit counted roughly 747 hardcoded `.dp` literals across the screen files, with four
 * different book-cover sizes (three of them off the declared 2:3 ratio) and six fixed heights
 * that clip at large font scales. There was no spacing scale, no elevation scale and no motion
 * scale to refer to, so every screen invented its own. These are the values screens are
 * expected to use from Phase 6 onward.
 */
internal object NovalPieSpacing {
    /** Hairline gaps: between a chip and its neighbour. */
    val xxs: Dp = 2.dp
    /** Inside a chip or pill. */
    val xs: Dp = 4.dp
    /** Between tightly related lines of text. */
    val sm: Dp = 8.dp
    /** Default gap between elements inside a card. */
    val md: Dp = 12.dp
    /** Card padding, and the gap between cards. */
    val lg: Dp = 16.dp
    /** Between major sections of a screen. */
    val xl: Dp = 24.dp
    /** Above a section that starts a new topic. */
    val xxl: Dp = 32.dp

    /** Horizontal page margin. One value, so screens line up with each other. */
    val screenHorizontal: Dp = 16.dp
    /**
     * Bottom padding for a scrolling list. Screens must NOT add their own bottom inset on top of
     * the Scaffold's: four screens previously stacked 80-96dp onto the Scaffold's own padding,
     * leaving up to ~160dp of dead space, while two others added none.
     */
    val listBottom: Dp = 16.dp
}

internal object NovalPieRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val pill: Dp = 999.dp
}

/**
 * Elevation. The app previously had exactly one explicit elevation, and cards measured 1.04:1
 * against the page in light mode, so nothing had a visible edge. Cards now separate by container
 * colour plus a hairline border; these values stay low so the UI does not look like floating
 * paper.
 */
internal object NovalPieElevation {
    val none: Dp = 0.dp
    val card: Dp = 1.dp
    val raised: Dp = 3.dp
    val dialog: Dp = 6.dp
}

internal object NovalPieSize {
    /** Minimum touch target. The old UI had 36dp star toggles and 32dp action icons. */
    val minTouchTarget: Dp = 48.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
    val avatarSm: Dp = 32.dp
    val avatarMd: Dp = 44.dp
    val hairline: Dp = 1.dp

    /** Book covers are 2:3 everywhere. Width varies by context; the ratio does not. */
    const val coverAspectRatio: Float = 2f / 3f
    val coverWidthGrid: Dp = 132.dp
    val coverWidthRow: Dp = 72.dp
    val coverWidthHero: Dp = 108.dp
}

/**
 * Motion. Navigation was a bare `when (route)` with no transition of any kind. These durations
 * are short enough not to feel sluggish on the route changes this app makes most often
 * (feed -> detail -> reader).
 */
internal object NovalPieMotion {
    const val fast: Int = 120
    const val medium: Int = 220
    const val slow: Int = 320

    /** Standard easing: decelerate into place. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

internal val NovalPieShapes = Shapes(
    extraSmall = RoundedCornerShape(NovalPieRadius.xs),
    small = RoundedCornerShape(NovalPieRadius.sm),
    medium = RoundedCornerShape(NovalPieRadius.md),
    large = RoundedCornerShape(NovalPieRadius.lg),
    extraLarge = RoundedCornerShape(NovalPieRadius.xl),
)
