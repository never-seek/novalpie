package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactLibraryRowMeasurementTest {
    @get:Rule val compose=createComposeRule()
    @Test fun narrowRowsReserveEnoughLinesForCompleteTitleAndAuthor() {
        var measured:CompactLibraryBookCardTextSlots?=null
        val books=listOf(NovelCard(1,"这是一个绝不能省略的很长的小说书名而且后面还有副标题","",author="作者的名字非常长也不能略掉"),NovelCard(2,"短书名",author="作者"))
        compose.setContent{MaterialTheme{val slots=compactLibraryRowTextSlots(books,68.dp);SideEffect{measured=slots}}}
        compose.runOnIdle{assertTrue(measured!!.titleLines>2);assertTrue(measured!!.authorLines>1)}
    }
}
