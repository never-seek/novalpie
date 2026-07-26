package com.novalpie.nativeapp.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app's colour system.
 *
 * Two problems in the previous theme are fixed here.
 *
 * First, completeness. The old theme passed 14 of Material 3's 36 colour roles to
 * `lightColorScheme()`/`darkColorScheme()`, and every omitted role silently kept Material's
 * baseline purple/pink value. That was visible: the message-centre hero blended the site blue
 * into baseline maroon `#7D5260` via the unbranded `tertiary`, and six error cards used baseline
 * pink `#F9DEDC`. Worse, Material 3 1.3 makes `surfaceContainerLow`/`surfaceContainerHighest`
 * the default container colour for `Card`, so roughly a hundred cards took a faintly
 * purple-tinted neutral instead of the app's own surface. [NovalPieColorTokens] has no default
 * values, so the compiler now refuses to let a role be forgotten.
 *
 * Second, contrast. Several of the old values failed WCAG AA outright: `onSurfaceVariant`
 * `#7D8A97` measured 3.53:1 on white across 103 call sites, white-on-`primary` measured 3.79:1
 * on every filled button, and dark mode's white-on-`#4D9DFF` measured 2.77:1 -- below even the
 * 3:1 non-text floor. The hues below stay in the site's blue-grey family, but lightness is
 * chosen so that each foreground/background pair clears AA. `ColorContrastTest` computes the
 * ratios and fails the build if a pair regresses, so this is enforced rather than asserted.
 */
internal data class NovalPieColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
)

/**
 * Light palette. Blue-grey, matching the source site's identity, with `primary` darkened from
 * the old `#3182ED` to `#1F6FE0` so that white button labels clear AA.
 */
internal val NovalPieLightColorTokens = NovalPieColorTokens(
    primary = Color(0xFF1F6FE0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE9FD),
    onPrimaryContainer = Color(0xFF0A3D8F),
    inversePrimary = Color(0xFFA9C9FA),
    secondary = Color(0xFF4E5A67),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E7EC),
    onSecondaryContainer = Color(0xFF333D48),
    // Teal rather than Material's maroon. tertiary is what the message-centre hero gradient
    // reads, so leaving it unbranded is what put maroon on screen.
    tertiary = Color(0xFF0F6E5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3EFE8),
    onTertiaryContainer = Color(0xFF05463A),
    error = Color(0xFFC02626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBDDDA),
    onErrorContainer = Color(0xFF7A1414),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1A2430),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2430),
    surfaceVariant = Color(0xFFEDF0F4),
    // Was #7D8A97 at 3.53:1. This is the most-used text colour in the app.
    onSurfaceVariant = Color(0xFF566270),
    surfaceTint = Color(0xFF1F6FE0),
    inverseSurface = Color(0xFF2B3542),
    inverseOnSurface = Color(0xFFF1F3F6),
    outline = Color(0xFF6C7885),
    outlineVariant = Color(0xFFD3D9E0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDCDFE4),
    // Card and friends default to these. Explicit values keep cards on the app's neutral ramp
    // instead of Material's tinted baseline.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF2F4F7),
    surfaceContainerHigh = Color(0xFFECEFF3),
    surfaceContainerHighest = Color(0xFFE5E9EE),
)

/**
 * Dark palette. Follows the Material 3 convention the old theme ignored: in dark mode `primary`
 * is a light tone carrying *dark* `onPrimary` text. The old scheme kept a mid blue with white
 * text on it, which is where the 2.77:1 button reading came from.
 */
internal val NovalPieDarkColorTokens = NovalPieColorTokens(
    primary = Color(0xFF8FBEFF),
    onPrimary = Color(0xFF06305F),
    primaryContainer = Color(0xFF14508F),
    onPrimaryContainer = Color(0xFFD3E4FF),
    inversePrimary = Color(0xFF1F6FE0),
    secondary = Color(0xFFB9C4D0),
    onSecondary = Color(0xFF24303C),
    secondaryContainer = Color(0xFF3A4550),
    onSecondaryContainer = Color(0xFFD6DEE8),
    tertiary = Color(0xFF74D6BF),
    onTertiary = Color(0xFF00382E),
    tertiaryContainer = Color(0xFF0B5748),
    onTertiaryContainer = Color(0xFF93F2DA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF12161B),
    onBackground = Color(0xFFE4E8ED),
    surface = Color(0xFF12161B),
    onSurface = Color(0xFFE4E8ED),
    surfaceVariant = Color(0xFF3F4750),
    onSurfaceVariant = Color(0xFFBFC7D1),
    surfaceTint = Color(0xFF8FBEFF),
    inverseSurface = Color(0xFFE4E8ED),
    inverseOnSurface = Color(0xFF2B3542),
    outline = Color(0xFF97A0AB),
    outlineVariant = Color(0xFF3F4750),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383C42),
    surfaceDim = Color(0xFF0D1014),
    surfaceContainerLowest = Color(0xFF0B0E12),
    surfaceContainerLow = Color(0xFF1A1E24),
    surfaceContainer = Color(0xFF1E232A),
    surfaceContainerHigh = Color(0xFF282D35),
    surfaceContainerHighest = Color(0xFF333840),
)

/**
 * Maps the tokens onto Material's [ColorScheme] one-to-one. Every role is supplied, so nothing
 * falls back to the baseline palette.
 */
internal fun NovalPieColorTokens.toColorScheme(): ColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainerLowest = surfaceContainerLowest,
)
