package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.NativeDownloadControl
import com.novalpie.nativeapp.feature.download.*
import com.novalpie.nativeapp.model.ReaderReplacementRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/** Opt-in live paid-download test. Uses the user-authorized account already logged into the app. */
class NativeDownloadLiveDeviceTest {
    @Test fun sourceAndReplacementEpubTxtUseNativeServiceAndProduceCompleteReadableFiles()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val arguments=InstrumentationRegistry.getArguments()
        val bookId=arguments.getString("bookId")?.toLongOrNull() ?: error("Live test requires an explicit, reviewed bookId")
        val app=instrumentation.targetContext.applicationContext
        val container=AppContainer.from(app)
        container.refreshEnvironmentFromStores()
        val user=container.api.currentUser()
        val book=container.api.bookDetail(bookId)
        assertNotNull("需要已登录账号",user.id)
        val scenario=ActivityScenario.launch(ComponentActivity::class.java)
        val evidence=File(app.cacheDir,"beta7-live-download").apply{mkdirs()}
        try {
            for(format in listOf(DownloadFormat.Txt,DownloadFormat.Epub))for(replace in listOf(false,true)) {
                val rules=if(replace)DownloadRulesSnapshot.encode(ReaderReplacementState(novelId=bookId,
                    personalRules=listOf(ReaderReplacementRule("qa-rule",bookId,"透明龙","透明龙【Beta7测试】")))) else null
                val retry=container.downloadStore.recover(user.id!!).tasks.firstOrNull {
                    it.bookId==bookId&&it.format==format&&it.applyReplacement==replace&&it.phase in setOf(DownloadPhase.Failed,DownloadPhase.NeedsRetry)&&it.authorizationFile!=null
                }
                val task=retry?.copy(phase=DownloadPhase.Queued) ?: DownloadTask(UUID.randomUUID().toString(),user.id!!,bookId,book.title,format,
                    applyReplacement=replace,replacementSnapshot=rules,requestedConcurrency=8)
                scenario.onActivity {NativeDownloadService.start(it,task)}
                val finished=withTimeout(120000){container.downloads.state.first {it.task?.id==task.id&&!it.busy}}
                assertEquals("下载失败：${finished.message ?: finished.task?.failure}",DownloadPhase.Completed,finished.task?.phase)
                val uri=android.net.Uri.parse(finished.task!!.destinationUri)
                val bytes=app.contentResolver.openInputStream(uri)!!.use{it.readBytes()}
                val name="${if(replace)"replaced"else"original"}.${if(format==DownloadFormat.Epub)"epub"else"txt"}"
                File(evidence,name).writeBytes(bytes)
                if(format==DownloadFormat.Txt) {
                    val text=bytes.toString(Charsets.UTF_8)
                    assertTrue(text.length>500)
                    if(replace)assertTrue(text.contains("透明龙【Beta7测试】"))
                } else {
                    val entries=mutableMapOf<String,ByteArray>()
                    ZipInputStream(bytes.inputStream()).use{zip->while(true){val entry=zip.nextEntry ?: break;entries[entry.name]=zip.readBytes()}}
                    assertEquals("application/epub+zip",entries["mimetype"]?.toString(Charsets.US_ASCII))
                    assertTrue(entries.containsKey("META-INF/container.xml"))
                    assertTrue(entries.keys.any{it.endsWith(".opf")})
                    val chapters=entries.filterKeys{it.endsWith(".xhtml")}.values.joinToString("\n"){it.toString(Charsets.UTF_8)}
                    assertTrue(chapters.length>500)
                    if(replace)assertTrue(chapters.contains("透明龙【Beta7测试】"))
                }
                // Only this test's newly created URI is removed from public Downloads. Evidence
                // stays in the app-private test folder for host inspection and later scoped cleanup.
                assertTrue(app.contentResolver.delete(uri,null,null)>0)
            }
        } finally {
            instrumentation.runOnMainSync{container.downloads.cancel()}
            scenario.close()
        }
    }
}
