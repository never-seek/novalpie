package com.novalpie.nativeapp.ui

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.feature.reader.text.DerivedTextPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.zip.ZipInputStream
import java.util.zip.CRC32
import org.json.JSONObject
import java.security.MessageDigest

/** Actual Android XML/ZIP/bitmap export tests with immutable local rules; no website calls or points. */
class NativeExportTextDeviceTest {
 @Test fun originalAndReplacedTxtEpubKeepTheRealImageOnceAndProtectHtmlAttributes()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val rule=ReaderReplacementRule("qa-personal",2,"Alice","__NOVALPIE_IMAGE_0__",target=ReaderReplacementTarget.Both)
  val shared=ReaderReplacementRule("qa-shared",2,"Alice","SHARED",owner=ReaderReplacementOwner.Shared,sharedRuleId="qa-shared")
  val state=ReaderReplacementState(novelId=2,personalRules=listOf(rule),sharedRules=LoadResult.Success(listOf(shared)),sharedRulesEnabledOverride=true)
  val frozen=readerDownloadReplacementSnapshot(true,state)
  val source="书名：Alice\n总章节数：1\n第1章 Alice\n<span title=\"Alice\">Alice</span>\n[图片: https://fixture.invalid/Alice.png]"
  val bitmap=Bitmap.createBitmap(3,2,Bitmap.Config.ARGB_8888).apply{eraseColor(0x402288ee)}
  val bytes=ByteArrayOutputStream().also{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray();bitmap.recycle()
  for(apply in listOf(false,true)) {
   val snapshot=if(apply)frozen else readerDownloadReplacementSnapshot(false,state)
   val txt=StringWriter();NativeEpubArchiveWriter.writeTransformedTxt(txt,StringReader(source),{n,t,b->snapshot.transform(n,t,b).toNativeDownloadText()})
   assertTrue(txt.toString().startsWith("书名：Alice\n总章节数：1\n"));assertTrue(txt.toString().contains("title=\"Alice\""));assertTrue(txt.toString().contains("https://fixture.invalid/Alice.png"))
   if(apply)assertTrue(txt.toString().contains(">__NOVALPIE_IMAGE_0__</span>"))else assertTrue(txt.toString().contains(">Alice</span>"))
   val out=ByteArrayOutputStream();NativeEpubArchiveWriter.write(out,NativeEpubMetadata("QA","Synthetic"),StringReader(source),
       openAsset={NativeEpubAsset("image/png",bytes.inputStream())},transformChapter={n,t,b->snapshot.transform(n,t,b).toNativeDownloadText()})
   val entries=linkedMapOf<String,ByteArray>();ZipInputStream(out.toByteArray().inputStream()).use{z->while(true){val e=z.nextEntry ?: break;val value=z.readBytes();entries[e.name]=value;assertEquals(e.crc,CRC32().apply{update(value)}.value)}}
   assertArrayEquals(bytes,entries.getValue("OEBPS/images/image-1.png"));assertEquals(1,entries.keys.count{it.startsWith("OEBPS/images/image-")})
   val chapter=entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
   File(context.cacheDir,"beta7-export-text-qa").apply{mkdirs()}.let {dir->File(dir,"chapter-${if(apply)"replaced"else "original"}.xhtml").writeText(chapter)}
   val xml=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(chapter.byteInputStream())
   assertEquals("Actual source illustration must appear once: apply=$apply; $chapter",1,xml.getElementsByTagName("img").length)
   assertTrue(chapter.contains("title=&quot;Alice&quot;"))
   if(apply)assertTrue(chapter.contains("__NOVALPIE_IMAGE_0__"))
   javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(chapter.byteInputStream())
  }
  val reader=DerivedTextPipeline.transform("<p title='Alice'>Alice</p>",listOf(rule),1)
  assertEquals("<p title='Alice'>__NOVALPIE_IMAGE_0__</p>",reader.markup)
  File(context.cacheDir,"beta7-export-text-qa").apply{mkdirs()}.let {dir->
   File(dir,"same-pipeline.json").writeText(JSONObject().put("passed",true).put("originalAndReplaced",true)
      .put("sourceImageSha",MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)})
      .put("noWebsiteWrites",true).toString())
  }
 }
 @Test fun headerlessTxtAppliesRulesWithoutFakeTitleAndUnchangedScriptOrUrl()=runBlocking {
  val state=ReaderReplacementState(novelId=2,personalRules=listOf(ReaderReplacementRule("qa",2,"Alice","Bob")))
  val snapshot=readerDownloadReplacementSnapshot(true,state);val out=StringWriter()
  NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader("Alice\n<script>Alice</script>\nhttps://fixture.invalid/Alice"),{n,t,b->snapshot.transform(n,t,b).toNativeDownloadText()})
  assertEquals("Bob\n<script>Alice</script>\nhttps://fixture.invalid/Alice\n",out.toString())
 }
}
