package com.novalpie.nativeapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.novalpie.nativeapp.feature.reader.pagination.NativePagedReader
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderViewportAnchor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class NativePaginationDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test fun realMeasuredPagesAdvanceReturnAndCrossTheBoundaryOnce() {
        val anchor=AtomicReference<ReaderViewportAnchor?>()
        val boundaries=CopyOnWriteArrayList<Int>()
        val chapterId=System.currentTimeMillis()
        val content=ReaderContent("分页测试","<p>这是一段必须跨越多页的中文长段落。🙂".plus("真实行边界不能截断，也不能跳过或重复。".repeat(90))+"</p><p>最后一段完整可见。</p>","test")
        compose.activityRule.scenario.onActivity {it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.setContent {
            MaterialTheme {
                var turn by remember {mutableStateOf<((Int)->Unit)?>(null)}
                Column(Modifier.fillMaxSize()) {
                    Row {
                        TextButton(onClick={turn?.invoke(-1)}){Text("测试上一页")}
                        TextButton(onClick={turn?.invoke(1)}){Text("测试下一页")}
                    }
                    NativePagedReader(900100,chapterId,content,content,
                        ReaderUiOptions(pageTurnMode=true,useInfiniteScroll=false,pageTurnEffect="none",showComments=false),
                        ReaderChapterEntryPosition.Start,null,FontFamily.Default,Color.Black,Color.White,true,true,
                        onTap={_,_->},onBoundary={boundaries.add(it)},registerTurn={turn=it},onAnchor={anchor.set(it)},onPreview={_,_->},
                        modifier=Modifier.weight(1f)){}
                }
            }
        }
        compose.waitUntil(15000){anchor.get()!=null}
        val first=anchor.get()
        compose.onNodeWithText("测试下一页").performClick()
        compose.waitUntil(5000){anchor.get()!=first}
        val second=anchor.get()
        compose.onNodeWithText("测试上一页").performClick()
        compose.waitUntil(5000){anchor.get()==first}
        assertEquals(first,anchor.get())
        compose.onNodeWithText("测试下一页").performClick()
        compose.waitUntil(5000){anchor.get()==second}
        repeat(70) {
            if(boundaries.isEmpty())compose.onNodeWithText("测试下一页").performClick()
        }
        assertEquals(listOf(1),boundaries)
        repeat(3){compose.onNodeWithText("测试下一页").performClick()}
        assertEquals("同一章尾不能重复发起跨章",listOf(1),boundaries)
    }
}
