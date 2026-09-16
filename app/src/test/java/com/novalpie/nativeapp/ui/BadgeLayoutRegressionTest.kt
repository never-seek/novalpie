package com.novalpie.nativeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.UserBadge
import org.junit.Assert.assertEquals
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
class BadgeLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalLayoutApi::class)
    @Test fun ordinaryBadgesMeasureTheirLabelsInsteadOfFillingTheAuthorColumn() {
        compose.setContent {
            MaterialTheme {
                FlowRow(Modifier.width(280.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("超级赛亚透明龙", "透明龙", "透明龙PROMAX").forEachIndexed { index, name ->
                        ProfileSourceBadge(
                            UserBadge(name = name, badgeCss = ".badge { padding:3px 10px; border-radius:9999px; background:linear-gradient(135deg,#22d3ee,#a855f7); }"),
                            ProfileBadgeDisplay.Inline,
                            Modifier.testTag("badge-$index"),
                        )
                    }
                }
            }
        }
        val first = compose.onNodeWithTag("badge-0").getBoundsInRoot()
        val second = compose.onNodeWithTag("badge-1").getBoundsInRoot()
        val third = compose.onNodeWithTag("badge-2").getBoundsInRoot()
        assertTrue("The first badge must not become an equal-width banner", first.right - first.left <= 180.dp)
        assertTrue("The short badge must keep its measured width", second.right - second.left <= 110.dp)
        assertTrue("The third badge must keep its measured width", third.right - third.left <= 180.dp)
        assertEquals("All three compact badges fit in this author column", first.top, third.top)
        assertEquals(first.top, second.top)
        assertTrue(second.left >= first.right)
        assertTrue(third.left >= second.right)
    }

    @Test fun imageBadgeKeepsTheSourceArtworkAspectRatio() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(240.dp)) {
                    ProfileSourceBadge(
                        UserBadge(name = "女杀孝子", badgeCss = ".badge { width:160px; min-width:160px; height:60px; padding:19px 22px 0; background:url('http://127.0.0.1:1/fixture.webp') center / contain no-repeat transparent; border:0; box-shadow:none; font-size:12px; }"),
                        ProfileBadgeDisplay.Hero,
                        Modifier.testTag("artwork"),
                    )
                }
            }
        }
        val bounds = compose.onNodeWithTag("artwork").getBoundsInRoot()
        assertEquals("Source 160x60 artwork must retain width", 160.dp, bounds.right - bounds.left)
        assertEquals("Source 160x60 artwork must retain height", 60.dp, bounds.bottom - bounds.top)
    }

    @Test fun profileHeaderStandardBadgesMayStretchButForumPillsDoNot() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(280.dp)) {
                    ProfileSourceBadge(
                        UserBadge(name = "透明龙"),
                        ProfileBadgeDisplay.Hero,
                        Modifier.testTag("profile-badge"),
                        stretchToParent = true,
                    )
                }
            }
        }
        val bounds = compose.onNodeWithTag("profile-badge").getBoundsInRoot()
        assertEquals(280.dp, bounds.right - bounds.left)
    }

    @Test fun narrowCardScalesTheWholeImageBadgeInsteadOfSquashingItsFrame() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(100.dp)) {
                    ProfileSourceBadge(
                        UserBadge(name = "女杀孝子", badgeCss = ".badge { width:160px; height:60px; padding:19px 22px 0; background:url('http://127.0.0.1:1/fixture.webp') center / contain no-repeat transparent; border:0; box-shadow:none; }"),
                        ProfileBadgeDisplay.Hero,
                        Modifier.testTag("narrow-artwork"),
                    )
                }
            }
        }
        val bounds = compose.onNodeWithTag("narrow-artwork").getBoundsInRoot()
        assertEquals(100f, (bounds.right - bounds.left).value, 0.5f)
        assertEquals("The frame and text must share the same fit factor", 37.5f, (bounds.bottom - bounds.top).value, 0.5f)
    }
}
