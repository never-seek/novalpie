package com.novalpie.nativeapp.ui

import androidx.compose.ui.graphics.Color
import com.novalpie.nativeapp.ui.design.NovalPieDarkColorTokens
import com.novalpie.nativeapp.ui.design.NovalPieLightColorTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Palette identity.
 *
 * This test previously pinned 14 exact colour values, including `primary = 0xFF3182ED`,
 * `onSurfaceVariant = 0xFF7D8A97` and dark `primary = 0xFF4D9DFF`. Those three are why it had to
 * change rather than merely grow: measured against the surfaces they are actually used on, they
 * are 3.79:1, 3.53:1 and 2.77:1 respectively, so all three failed WCAG AA and the last fell below
 * even the 3:1 non-text floor. Pinning them meant pinning an accessibility defect in place.
 *
 * The value-level guarantee now lives in `design/ColorContrastTest`, which computes ratios rather
 * than hardcoding hexes. What this test protects is the *identity* those values encoded — that the
 * app is blue-grey, matching the source website, not Material's default purple — plus the
 * structural properties a palette must satisfy to be usable.
 */
class ThemePaletteTest {

    /** Hue in degrees: 0 red, 120 green, 240 blue. */
    private fun hue(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta < 0.0001f) return 0f
        val h = when (max) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return (h + 360f) % 360f
    }

    private fun saturation(color: Color): Float {
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        return if (max < 0.0001f) 0f else (max - min) / max
    }

    @Test
    fun lightAccentStaysInTheWebsiteBlueFamily() {
        val hue = hue(NovalPieLightColorTokens.primary)
        assertTrue("light primary hue $hue is not blue", hue in 200f..230f)
        assertTrue(
            "light primary is too desaturated to read as the brand accent",
            saturation(NovalPieLightColorTokens.primary) > 0.5f,
        )
    }

    @Test
    fun darkAccentStaysInTheWebsiteBlueFamily() {
        val hue = hue(NovalPieDarkColorTokens.primary)
        assertTrue("dark primary hue $hue is not blue", hue in 200f..230f)
    }

    /**
     * The old palette's defining characteristic: cool blue-grey neutrals rather than Material's
     * purple-tinted ones. Guards the regression where an omitted role reintroduced a purple cast.
     */
    @Test
    fun neutralsAreCoolNotPurple() {
        val neutrals = listOf(
            "light surfaceVariant" to NovalPieLightColorTokens.surfaceVariant,
            "light onSurfaceVariant" to NovalPieLightColorTokens.onSurfaceVariant,
            "light onSurface" to NovalPieLightColorTokens.onSurface,
            "light outline" to NovalPieLightColorTokens.outline,
            "light surfaceContainer" to NovalPieLightColorTokens.surfaceContainer,
            "dark surfaceVariant" to NovalPieDarkColorTokens.surfaceVariant,
            "dark onSurfaceVariant" to NovalPieDarkColorTokens.onSurfaceVariant,
            "dark surfaceContainer" to NovalPieDarkColorTokens.surfaceContainer,
        )
        for ((name, color) in neutrals) {
            // A cool grey has blue >= red and green >= red. A purple cast shows up as red
            // exceeding green.
            assertTrue(
                "$name is warm/purple-tinted (r=${color.red} g=${color.green} b=${color.blue})",
                color.blue >= color.red && color.green >= color.red,
            )
        }
    }

    @Test
    fun lightAndDarkAreDistinctAndSelfConsistent() {
        assertNotEquals(NovalPieLightColorTokens.background, NovalPieDarkColorTokens.background)
        assertNotEquals(NovalPieLightColorTokens.surface, NovalPieDarkColorTokens.surface)
        assertNotEquals(NovalPieLightColorTokens.primary, NovalPieDarkColorTokens.primary)

        // primary and secondary must be tellable apart -- the original test asserted this too.
        assertNotEquals(NovalPieLightColorTokens.primary, NovalPieLightColorTokens.secondary)
        assertNotEquals(NovalPieDarkColorTokens.primary, NovalPieDarkColorTokens.secondary)

        // Trivial, but it is exactly what broke when styles.xml inherited the dark platform theme
        // for a light app.
        assertTrue(
            "light background should be brighter than light onBackground",
            NovalPieLightColorTokens.background.green > NovalPieLightColorTokens.onBackground.green,
        )
        assertTrue(
            "dark background should be darker than dark onBackground",
            NovalPieDarkColorTokens.background.green < NovalPieDarkColorTokens.onBackground.green,
        )
    }

    /**
     * Material 3 convention the old dark palette violated: in dark mode the accent is a light tone
     * carrying dark text. A mid-tone accent with white text is what produced the 2.77:1 button.
     */
    @Test
    fun darkModeAccentCarriesDarkForeground() {
        val primaryBrightness = NovalPieDarkColorTokens.primary.green
        val onPrimaryBrightness = NovalPieDarkColorTokens.onPrimary.green
        assertTrue(
            "dark onPrimary ($onPrimaryBrightness) should be darker than dark primary ($primaryBrightness)",
            onPrimaryBrightness < primaryBrightness,
        )
    }

    /** The surface container ramp must be monotonic or elevation reads inconsistently. */
    @Test
    fun surfaceContainerRampIsMonotonic() {
        val light = NovalPieLightColorTokens
        val lightRamp = listOf(
            light.surfaceContainerLowest,
            light.surfaceContainerLow,
            light.surfaceContainer,
            light.surfaceContainerHigh,
            light.surfaceContainerHighest,
        ).map { it.green }
        assertEquals(
            "light surface container ramp should darken monotonically",
            lightRamp.sortedDescending(),
            lightRamp,
        )

        val dark = NovalPieDarkColorTokens
        val darkRamp = listOf(
            dark.surfaceContainerLowest,
            dark.surfaceContainerLow,
            dark.surfaceContainer,
            dark.surfaceContainerHigh,
            dark.surfaceContainerHighest,
        ).map { it.green }
        assertEquals(
            "dark surface container ramp should lighten monotonically",
            darkRamp.sorted(),
            darkRamp,
        )
    }
}
