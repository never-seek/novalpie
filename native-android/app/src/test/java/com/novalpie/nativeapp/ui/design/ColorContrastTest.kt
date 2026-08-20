package com.novalpie.nativeapp.ui.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Enforces the colour system's two invariants rather than trusting that they hold.
 *
 * The palette this replaced failed WCAG AA in three places that shipped: `onSurfaceVariant`
 * at 3.53:1 across 103 call sites, white-on-`primary` at 3.79:1 on every filled button, and
 * dark-mode white-on-`primary` at 2.77:1. Those were not caught because nothing measured them.
 * This test measures them.
 */
class ColorContrastTest {

    /** WCAG 2.1 relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** WCAG 2.1 contrast ratio, always >= 1. */
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertRatio(label: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrast(foreground, background)
        assertTrue(
            "$label: %.2f:1 but needs %.1f:1".format(ratio, minimum),
            ratio >= minimum,
        )
    }

    /** AA for normal-size text. Everything the app renders as body/label copy must clear this. */
    private val aaText = 4.5

    /** AA for UI component boundaries and large text. */
    private val aaNonText = 3.0

    private fun assertTextPairs(name: String, tokens: NovalPieColorTokens) {
        // Container/on-container pairs. Material guarantees these are used together.
        assertRatio("$name onPrimary on primary", tokens.onPrimary, tokens.primary, aaText)
        assertRatio("$name onSecondary on secondary", tokens.onSecondary, tokens.secondary, aaText)
        assertRatio("$name onTertiary on tertiary", tokens.onTertiary, tokens.tertiary, aaText)
        assertRatio("$name onError on error", tokens.onError, tokens.error, aaText)
        assertRatio("$name onBackground on background", tokens.onBackground, tokens.background, aaText)
        assertRatio("$name onSurface on surface", tokens.onSurface, tokens.surface, aaText)
        assertRatio(
            "$name onPrimaryContainer on primaryContainer",
            tokens.onPrimaryContainer, tokens.primaryContainer, aaText,
        )
        assertRatio(
            "$name onSecondaryContainer on secondaryContainer",
            tokens.onSecondaryContainer, tokens.secondaryContainer, aaText,
        )
        assertRatio(
            "$name onTertiaryContainer on tertiaryContainer",
            tokens.onTertiaryContainer, tokens.tertiaryContainer, aaText,
        )
        assertRatio(
            "$name onErrorContainer on errorContainer",
            tokens.onErrorContainer, tokens.errorContainer, aaText,
        )
        assertRatio(
            "$name onSurfaceVariant on surfaceVariant",
            tokens.onSurfaceVariant, tokens.surfaceVariant, aaText,
        )
        assertRatio(
            "$name inverseOnSurface on inverseSurface",
            tokens.inverseOnSurface, tokens.inverseSurface, aaText,
        )

        // onSurfaceVariant is the app's most-used text colour and appears on the plain surface
        // and on the page background too, not only on surfaceVariant. All three must clear AA.
        assertRatio(
            "$name onSurfaceVariant on surface",
            tokens.onSurfaceVariant, tokens.surface, aaText,
        )
        assertRatio(
            "$name onSurfaceVariant on background",
            tokens.onSurfaceVariant, tokens.background, aaText,
        )

        // Body text also lands directly on card containers.
        assertRatio(
            "$name onSurface on surfaceContainer",
            tokens.onSurface, tokens.surfaceContainer, aaText,
        )
        assertRatio(
            "$name onSurface on surfaceContainerHighest",
            tokens.onSurface, tokens.surfaceContainerHighest, aaText,
        )
        assertRatio(
            "$name onSurfaceVariant on surfaceContainer",
            tokens.onSurfaceVariant, tokens.surfaceContainer, aaText,
        )

        // The error colour is used as standalone text on plain surfaces in 14 places.
        assertRatio("$name error on surface", tokens.error, tokens.surface, aaText)
    }

    private fun assertBoundaryPairs(name: String, tokens: NovalPieColorTokens) {
        // Card and outlined-control edges were 1.04:1 light and 1.27:1 dark, i.e. invisible.
        assertRatio("$name outline on surface", tokens.outline, tokens.surface, aaNonText)
        assertRatio("$name outline on background", tokens.outline, tokens.background, aaNonText)
        // outlineVariant is the divider tone. It is decorative, so it only needs to be
        // perceptible, not AA -- but it must not be invisible either.
        assertTrue(
            "$name outlineVariant is indistinguishable from surface",
            contrast(tokens.outlineVariant, tokens.surface) >= 1.15,
        )
        // Filled primary must be discernible against the page it sits on.
        assertRatio("$name primary on background", tokens.primary, tokens.background, aaNonText)
    }

    @Test
    fun lightPaletteMeetsWcagAa() {
        assertTextPairs("light", NovalPieLightColorTokens)
        assertBoundaryPairs("light", NovalPieLightColorTokens)
    }

    @Test
    fun darkPaletteMeetsWcagAa() {
        assertTextPairs("dark", NovalPieDarkColorTokens)
        assertBoundaryPairs("dark", NovalPieDarkColorTokens)
    }

    /**
     * Guards the specific regression that put Material's baseline palette on screen. If a role
     * is ever dropped back to a default, it reappears as one of these values.
     */
    @Test
    fun noRoleUsesMaterialBaselinePurpleOrPink() {
        val baselineLeaks = mapOf(
            0xFF6750A4 to "baseline primary (purple)",
            0xFFEADDFF to "baseline primaryContainer (pale purple)",
            0xFF625B71 to "baseline secondary (purple-grey)",
            0xFFE8DEF8 to "baseline secondaryContainer (pale purple)",
            0xFF7D5260 to "baseline tertiary (maroon)",
            0xFFFFD8E4 to "baseline tertiaryContainer (pink)",
            0xFFEFB8C8 to "baseline dark tertiary (pink)",
            0xFFF9DEDC to "baseline errorContainer (pink)",
            0xFFFFFBFE to "baseline surface (purple-tinted white)",
            0xFFCAC4D0 to "baseline outlineVariant (purple-grey)",
        )

        for ((name, tokens) in listOf(
            "light" to NovalPieLightColorTokens,
            "dark" to NovalPieDarkColorTokens,
        )) {
            for (color in tokens.allColors()) {
                val packed = color.toArgbLong()
                val leak = baselineLeaks[packed]
                assertTrue(
                    "$name palette contains %s (#%08X)".format(leak ?: "", packed),
                    leak == null,
                )
            }
        }
    }

    /**
     * The point of [NovalPieColorTokens] is that the compiler forbids omitting a role. This
     * pins the count so that adding a Material role without supplying a value is a test
     * failure rather than a silent baseline fallback.
     */
    @Test
    fun everyMaterialColorRoleIsSupplied() {
        assertEquals(36, NovalPieLightColorTokens.allColors().size)
        assertEquals(36, NovalPieDarkColorTokens.allColors().size)

        val scheme = NovalPieLightColorTokens.toColorScheme()
        assertEquals(NovalPieLightColorTokens.primary, scheme.primary)
        assertEquals(NovalPieLightColorTokens.tertiary, scheme.tertiary)
        assertEquals(NovalPieLightColorTokens.errorContainer, scheme.errorContainer)
        assertEquals(NovalPieLightColorTokens.surfaceContainerLow, scheme.surfaceContainerLow)
        assertEquals(NovalPieLightColorTokens.outlineVariant, scheme.outlineVariant)
    }
}

private fun Color.toArgbLong(): Long {
    fun component(value: Float): Long = (value * 255f + 0.5f).toLong().coerceIn(0, 255)
    return (0xFFL shl 24) or
        (component(red) shl 16) or
        (component(green) shl 8) or
        component(blue)
}

private fun NovalPieColorTokens.allColors(): List<Color> = listOf(
    primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary,
    secondary, onSecondary, secondaryContainer, onSecondaryContainer,
    tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
    error, onError, errorContainer, onErrorContainer,
    background, onBackground, surface, onSurface,
    surfaceVariant, onSurfaceVariant, surfaceTint,
    inverseSurface, inverseOnSurface,
    outline, outlineVariant, scrim,
    surfaceBright, surfaceDim,
    surfaceContainerLowest, surfaceContainerLow, surfaceContainer,
    surfaceContainerHigh, surfaceContainerHighest,
)
