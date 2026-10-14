package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
class ForumRichUiRegressionTest {
    @get:Rule val compose=createComposeRule()
    @Test fun surroundingTextDoesNotFlattenTheRevealedRichSpoiler() {
        compose.setContent {MaterialTheme {ForumRichContent("Outside ||inside [bookid:123] HIDDEN_TAIL|| end",
            MaterialTheme.typography.bodyMedium,{},emptyLabel="empty",hideSpoilers=true)}}
        compose.onNodeWithText("Outside",substring=true).assertExists()
        compose.onNodeWithText("HIDDEN_TAIL",substring=true).assertDoesNotExist()
        compose.onNodeWithText("点击揭示剧透内容").performClick()
        compose.onNodeWithText("书籍 #123",substring=true).assertExists()
        compose.onNodeWithText("HIDDEN_TAIL",substring=true).assertExists()
    }
    @Test fun spoilerContainingBookRevealsItsRichCardOnlyOnDemand() {
        val opened=mutableListOf<String>()
        compose.setContent {MaterialTheme {ForumRichContent("||SECRET_BEFORE [bookid:123] SECRET_AFTER||",
            MaterialTheme.typography.bodyMedium,{opened+=it},emptyLabel="empty",hideSpoilers=true)}}
        compose.onNodeWithText("SECRET_AFTER",substring=true).assertDoesNotExist()
        compose.onNodeWithText("点击揭示剧透内容").performClick()
        compose.onNodeWithText("SECRET_AFTER",substring=true).assertExists()
        compose.onNodeWithText("书籍 #123",substring=true).assertExists()
    }
    @Test fun nestedFoldsRemainClosedUntilEachOwnHeaderIsClicked() {
        compose.setContent {MaterialTheme {ForumRichContent("[fold:Outer]Before [fold:Inner]INNER_SECRET[/fold] OUTER_TAIL[/fold]",
            MaterialTheme.typography.bodyMedium,{},emptyLabel="empty")}}
        compose.onNodeWithText("OUTER_TAIL",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Outer").performClick()
        compose.onNodeWithText("OUTER_TAIL",substring=true).assertExists()
        compose.onNodeWithText("INNER_SECRET",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Inner").performClick()
        compose.onNodeWithText("INNER_SECRET",substring=true).assertExists()
    }
    @Test fun globalSpoilerPreferenceCanRevealRichContentsWithoutClick() {
        var hide by mutableStateOf(true)
        compose.setContent {MaterialTheme {ForumRichContent("||Before [bookid:123] AFTER_MARK||",MaterialTheme.typography.bodyMedium,{},emptyLabel="empty",hideSpoilers=hide)}}
        compose.onNodeWithText("AFTER_MARK",substring=true).assertDoesNotExist()
        compose.runOnIdle {hide=false}
        compose.onNodeWithText("AFTER_MARK",substring=true).assertExists()
    }
    @Test fun createPreviewUsesRichTextRatherThanRawMarkdown() {
        compose.setContent {MaterialTheme {ForumCreateScreen(ForumCreateState(draft=ForumCreateDraft(type="discussion",title="QA",content="**BOLD_MARK**")),{}, {}, {})}}
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("正文预览"))
        val raw=compose.onAllNodes(SemanticsMatcher("raw Markdown displayed as plain Text") {
            it.config.getOrNull(SemanticsProperties.Text)?.any {v->v.text=="**BOLD_MARK**"}==true
        }).fetchSemanticsNodes()
        assertTrue(raw.isEmpty())
        compose.onNodeWithText("BOLD_MARK").assertExists()
    }
}
