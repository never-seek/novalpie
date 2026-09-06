package com.novalpie.nativeapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.feature.messages.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException

/** All messages are synthetic and stay on-device: these tests never read or send real DMs. */
class MessageFeatureDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    private val models = mutableListOf<MessageFeature>()
    private open class Repository : MessagesRepository {
        override suspend fun page(query: MessageQuery, page: Int, pageSize: Int) = MessagePage(emptyList(), MessagePagination())
        override suspend fun stats() = MessageStats()
        override suspend fun detail(id: Long) = SiteMessage(id, 1, "合成通知")
        override suspend fun markRead(ids: List<Long>) = MessageActionResult(true)
        override suspend fun markAllRead() = MessageActionResult(true)
        override suspend fun star(id: Long, starred: Boolean) = MessageActionResult(true)
        override suspend fun delete(ids: List<Long>) = MessageActionResult(true)
        override suspend fun conversation(targetId: Long, page: Int, pageSize: Int) = listOf(DirectMessage(1, "这是自动验收的合成对话，不是真实私信", executeUserId = 2))
        override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) = MessageActionResult(true)
        override suspend fun settings() = MessageSettings()
        override suspend fun saveSettings(settings: MessageSettings) = MessageActionResult(true)
    }
    @After fun closeFeatures() { compose.runOnIdle { models.forEach(MessageFeature::close) } }

    @Test fun refreshingAndFailedSendingLeaveTheComposerUsableAndVisible() {
        val model = ConversationViewModel(object : Repository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String): MessageActionResult = throw IOException("synthetic")
        }).also(models::add)
        compose.setContent { MaterialTheme {
            MessageConversationScreen(model.state, 1, { model.load(2, "验收收件人") }, model::edit, { model.send(1, "验收") }, model::loadMore)
        } }
        compose.runOnIdle { model.load(2, "验收收件人") }
        compose.onNode(hasSetTextAction()).performTextInput("Beta7 synthetic draft")
        compose.onNodeWithContentDescription("刷新").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Beta7 synthetic draft")
        compose.onNodeWithContentDescription("发送").performClick()
        compose.waitUntil(5000) { model.state.actionMessage?.contains("结果未确认") == true }
        compose.onNode(hasSetTextAction()).assertTextContains("Beta7 synthetic draft").assertIsDisplayed()
        compose.onNodeWithContentDescription("发送").assertIsDisplayed()
        capture("message-conversation-draft.png")
    }

    @Test fun sendingToOneRecipientCannotEraseAnotherRecipientsDraft() {
        val ack = CompletableDeferred<MessageActionResult>()
        val model = ConversationViewModel(object : Repository() {
            override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) = ack.await()
        }).also(models::add)
        compose.setContent { MaterialTheme { Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { model.load(3, "验收收件人乙") }) { androidx.compose.material3.Text("切换收件人") }
            MessageConversationScreen(model.state, 1, { model.load(model.state.targetUserId, model.state.targetName) }, model::edit, { model.send(1, "验收") }, model::loadMore)
        } } }
        compose.runOnIdle { model.load(2, "验收收件人甲") }
        compose.onNode(hasSetTextAction()).performTextInput("first synthetic")
        compose.onNodeWithContentDescription("发送").performClick()
        compose.onNodeWithText("切换收件人").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("second synthetic draft")
        compose.runOnIdle { ack.complete(MessageActionResult(true)) }
        compose.onNodeWithText("验收收件人乙").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("second synthetic draft").assertIsDisplayed()
        assertEquals(3L, model.state.targetUserId)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        File(folder, name).outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
