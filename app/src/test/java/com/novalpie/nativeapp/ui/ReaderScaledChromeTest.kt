package com.novalpie.nativeapp.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderScaledChromeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun chromeBudgetContainsTheActualLabelLineAtLargeSystemFontScale() {
        lateinit var measurer: TextMeasurer
        compose.setContent { val value = rememberTextMeasurer(); SideEffect { measurer = value } }
        compose.runOnIdle {
            for (scale in listOf(1f, 1.3f, 2f)) {
                val density = Density(1.5f, scale)
                val style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
                val measured = measurer.measure("23:59 99.99% 章节", style = style, density = density)
                val chrome = readerChromeLayout(with(density) { style.lineHeight.toDp().value })
                assertTrue("fontScale=$scale footer clips actual line", chrome.statusHeightDp * density.density >= measured.size.height)
                assertTrue("fontScale=$scale header clips actual line", chrome.headerHeightDp * density.density >= measured.size.height)
            }
        }
    }
}
