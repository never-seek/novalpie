package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.*
import coil.Coil
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** A real failed HTTP illustration must retry without leaving the chapter or opening the toolbar. */
class ReaderIllustrationRetryDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<ReaderTestActivity>()
 @Test fun continuousIllustrationCanRetryItsRealRequestWithoutOpeningTheReaderMenu() {
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  val previous=Coil.imageLoader(app)
  val loader=buildNovalPieImageLoader(app,ProxySettings(enabled=false),emulatorRuntime=false)
  Coil.setImageLoader(loader)
  val requests=AtomicInteger()
  val server=ServerSocket(0)
  val png=ByteArrayOutputStream().also{out->
   android.graphics.Bitmap.createBitmap(4,4,android.graphics.Bitmap.Config.ARGB_8888).let { bitmap->
    bitmap.eraseColor(android.graphics.Color.BLUE);bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);bitmap.recycle()
   }
  }.toByteArray()
  val worker=thread(isDaemon=true,name="reader-image-retry-fixture") {
   runCatching { while(!server.isClosed) server.accept().use { socket->
    val input=socket.getInputStream().bufferedReader()
    while(true){val line=input.readLine()?:break;if(line.isEmpty())break}
    val attempt=requests.incrementAndGet()
    val body=if(attempt==1)"temporary image failure".toByteArray() else png
    val status=if(attempt==1)"503 Service Unavailable" else "200 OK"
    socket.getOutputStream().apply{write("HTTP/1.1 $status\r\nContent-Type: image/png\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray());write(body);flush()}
   }}
  }
  try {
   val content=ReaderContent("插图重试章","<p>用于测试真实图片失败和重试。</p><img src=\"http://127.0.0.1:${server.localPort}/image.png\" />","fixture")
   compose.setContent{MaterialTheme{ReaderScreen(
    state=ReaderState(bookId=990501,chapterId=990502,bookTitle="受控插图重试",content=LoadResult.Success(content),chapterContents=listOf(ReaderChapterContent(990502,"插图重试章",content)),chapters=LoadResult.Success(listOf(Chapter(990502,"插图重试章",1)))),
    options=ReaderUiOptions(useInfiniteScroll=true,showTts=false,showComments=false,pageTurnEffect="none"),readerFullscreen=false,onReaderFullscreenChange={},ttsSettings=ReaderTtsSettings(),
    replacementState=ReaderReplacementState(novelId=990501),catalogQuery="",onCatalogQueryChange={},
    onDecreaseFont={},onIncreaseFont={},onCycleTheme={},onReaderOptionsChange={},onReaderTtsSettingsChange={},
    onReaderReplacementSourceChange={},onSaveReaderReplacementRule={},onDeleteReaderReplacementRule={},onSetSharedReaderReplacementRuleVisible={_,_->},onCloneSharedReaderReplacementRule={},onReaderSharedRulesEnabledChange={},onDefaultReaderSharedRulesEnabledChange={},onResetReaderSharedRulesOverride={},onResetReaderOptions={},onClearReaderChapterCache={},onRetry={},onRetryChapterComments={},onRetryCatalog={},onOpenReader={_,_->},onOpenReaderAtPosition={_,_,_->},onLoadNextChapter={},onLoadPreviousChapter={},onVisibleChapterChanged={_,_->},onViewportAnchorChanged={},onToggleFavorite={},onBack={},onCommentDraftChange={_,_->},onSubmitComment={},onReplyComment={_,_->},onCancelCommentReply={},onCommentLike={_,_->},onCommentDislike={_,_->},onCommentEmoji={_,_->},onCommentAward={_,_->},onOpenUser={},onOpenLink={},onOpenWeb={},onPreviewImage={_,_->},recordPlaybackProgress=false
   )}}
   compose.waitUntil(15000){compose.onAllNodesWithText("插图加载失败").fetchSemanticsNodes().isNotEmpty()}
   compose.onNodeWithText("重试插图").assertIsDisplayed().performTouchInput{click()}
   compose.waitUntil(15000){requests.get()>=2 && compose.onAllNodesWithText("插图加载失败").fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText("正在加载插图").fetchSemanticsNodes().isEmpty()}
   compose.onNodeWithText("设置").assertDoesNotExist()
   assertTrue(requests.get()>=2)
  } finally {
   server.close();worker.join(1000);Coil.setImageLoader(previous);loader.shutdown()
  }
 }
}
