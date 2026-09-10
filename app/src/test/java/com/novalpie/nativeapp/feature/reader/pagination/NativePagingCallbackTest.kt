package com.novalpie.nativeapp.feature.reader.pagination

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.ui.ReaderChapterEntryPosition
import com.novalpie.nativeapp.ui.ReaderUiOptions
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativePagingCallbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun catalogArrivingAfterBodyMustEnablePreviousChapterWithoutRecreatingReader() {
        verifyLateCatalogue(-1)
    }
    @Test fun catalogArrivingAfterBodyMustEnableNextChapterWithoutRecreatingReader() {
        verifyLateCatalogue(1)
    }
    private fun verifyLateCatalogue(direction: Int) {
        var catalogReady by mutableStateOf(false)
        val turn = AtomicReference<((Int) -> Unit)?>(null)
        val ready = AtomicBoolean(false)
        val boundaries = mutableListOf<Int>()
        val content = ReaderContent("第二章", "<p>首段正文。</p>", "fixture")
        compose.setContent { MaterialTheme {
            NativePagedReader(900123, 2, content, content,
                ReaderUiOptions(pageTurnMode = true, showComments = false, pageTurnEffect = "none"),
                ReaderChapterEntryPosition.Start, null, FontFamily.Default, Color.Black, Color.White,
                hasPrevious = catalogReady && direction < 0, hasNext = catalogReady && direction > 0, onTap = { _, _ -> },
                onBoundary = { boundaries += it }, registerTurn = { turn.set(it) }, onAnchor = { ready.set(true) },
                onPreview = { _, _ -> }, modifier = Modifier.size(360.dp, 600.dp)) {}
        } }
        compose.waitUntil(15000) { ready.get() && turn.get() != null }
        compose.runOnIdle { turn.get()!!(direction) }
        assertTrue(boundaries.isEmpty())
        compose.runOnIdle { catalogReady = true }
        compose.waitForIdle()
        compose.runOnIdle { turn.get()!!(direction) }
        assertEquals("目录迟到后必须使用新的上下章状态", listOf(direction), boundaries)
        compose.runOnIdle { turn.get()!!(direction) }
        assertEquals(listOf(direction), boundaries)
    }
}
