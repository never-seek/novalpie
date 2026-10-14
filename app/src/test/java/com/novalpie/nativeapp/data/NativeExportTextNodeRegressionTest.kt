package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.zip.ZipInputStream

class NativeExportTextNodeRegressionTest {
 private fun snapshot(target:String)=readerDownloadReplacementSnapshot(true,ReaderReplacementState(novelId=2,
    personalRules=listOf(ReaderReplacementRule("qa",2,"Alice",target,target=ReaderReplacementTarget.Both))))
 private suspend fun txt(source:String,target:String="Bob"):String {
  val out=StringWriter();val rules=snapshot(target)
  NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader(source),{n,t,b->rules.transform(n,t,b).toNativeDownloadText()})
  return out.toString()
 }
 private suspend fun epub(source:String,target:String="Bob"):Map<String,ByteArray> {
  val out=ByteArrayOutputStream();val rules=snapshot(target)
  NativeEpubArchiveWriter.write(out,NativeEpubMetadata("QA","Synthetic"),StringReader(source),
    openAsset={NativeEpubAsset("image/png",ByteArrayInputStream(PNG))},
    transformChapter={n,t,b->rules.transform(n,t,b).toNativeDownloadText()})
  val entries=linkedMapOf<String,ByteArray>();ZipInputStream(out.toByteArray().inputStream()).use{z->while(true){val e=z.nextEntry ?: break;entries[e.name]=z.readBytes()}}
  return entries
 }
 @Test fun txtReplacementChangesOnlyVisibleNodesNotHtmlAttributesOrUrls()=runBlocking {
  val raw="第1章\n<span title=\"Alice\">Alice <a href=\"https://fixture.invalid/Alice?q=Alice\">Alice</a></span>"
  val result=txt(raw)
  assertTrue(result.contains("title=\"Alice\""));assertTrue(result.contains("q=Alice"));assertTrue(result.contains(">Bob <a"));assertTrue(result.contains(">Bob</a>"))
 }
 @Test fun txtScriptCommentAndImageDestinationsRemainOriginalWhileLinkLabelChanges()=runBlocking {
  val result=txt("第1章\nAlice <!-- Alice --> <script>Alice</script> [Alice](https://fixture.invalid/Alice) [图片: https://fixture.invalid/Alice.png]")
  assertTrue(result.contains("<!-- Alice -->"));assertTrue(result.contains("<script>Alice</script>"));assertTrue(result.contains("[Bob](https://fixture.invalid/Alice)"))
  assertTrue(result.contains("[图片: https://fixture.invalid/Alice.png]"))
 }
 @Test fun headerlessBodyAppliesRulesWithoutInventingAHeading()=runBlocking {
  assertEquals("Bob\nBody\n",txt("Alice\nBody"))
 }
 @Test fun headerlessCallbackTitleDoesNotBecomeAnAddedFakeChapter()=runBlocking {
  val out=StringWriter();var called=0
  NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader("Alice\nBody"),{n,t,b->called++;assertEquals(1,n);assertEquals("",t);NativeDownloadChapterText("FAKE_TITLE",b.replace("Alice","Bob"))})
  assertEquals(1,called);assertEquals("Bob\nBody\n",out.toString())
 }
 @Test fun recognizedChapterKeepsItsMetadataPreambleUnreplaced()=runBlocking {
  val result=txt("书名：Alice\n作者：Alice\n第1章 Alice\nAlice")
  assertTrue(result.startsWith("书名：Alice\n作者：Alice\n"));assertTrue(result.contains("第1章 Bob\nBob"))
 }
 @Test fun emptyInputDoesNotCreateOrTransformAnInventedChapter()=runBlocking {
  val out=StringWriter()
  NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader(""),{_,_,_->error("No chapter exists")})
  assertEquals("",out.toString())
 }
 @Test fun replacementOutputCannotBecomeAnExtraInternalImageReference()=runBlocking {
  val entries=epub("第1章\nAlice\n[图片: https://fixture.invalid/a.png]","__NOVALPIE_IMAGE_0__")
  val body=entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
  assertEquals(1,Regex("<img\\b").findAll(body).count());assertTrue(body.contains("__NOVALPIE_IMAGE_0__"))
  assertArrayEquals(PNG,entries.getValue("OEBPS/images/image-1.png"))
 }
 @Test fun authoredTextThatLooksLikeAnInternalTokenIsStillJustText()=runBlocking {
  val entries=epub("第1章\n__NOVALPIE_IMAGE_0__ is a code example\n[图片: https://fixture.invalid/a.png]")
  val body=entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
  assertEquals(1,Regex("<img\\b").findAll(body).count());val visible=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(body.byteInputStream()).documentElement.textContent
  // Authored underscores are source Markdown emphasis; the existing formatter removes them.
  // They must never be consumed a second time as internal image structure.
  assertTrue(visible.contains("NOVALPIEIMAGE0 is a code example"))
 }
 @Test fun epubVisibleReplacementDoesNotRewriteSourceHtmlAttributes()=runBlocking {
  val entries=epub("第1章\n<span title=\"Alice\">Alice</span>")
  val xml=entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
  assertTrue(xml.contains("title=&quot;Alice&quot;"));assertFalse(xml.contains("title=&quot;Bob&quot;"));assertTrue(xml.contains("Bob"))
 }

 @Test fun largeMetadataPrefixSpoolsWithoutTransformingItAndCleansOnlyItsOwnTemp()=runBlocking {
  val directory=java.nio.file.Files.createTempDirectory("qa-preamble-").toFile()
  val unrelated=File(directory,"user.txt").apply{writeText("USER")}
  val prefix="Alice metadata ".repeat(7000)
  val out=StringWriter();val rules=snapshot("Bob")
  try {
   NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader(prefix+"\n第1章 Alice\nAlice"),{n,t,b->rules.transform(n,t,b).toNativeDownloadText()},preambleDirectory=directory)
   assertTrue(out.toString().startsWith(prefix+"\n"));assertTrue(out.toString().contains("第1章 Bob\nBob"))
   assertEquals(listOf("user.txt"),directory.listFiles()!!.map{it.name});assertEquals("USER",unrelated.readText())
  }finally{unrelated.delete();directory.delete()}
 }
 @Test fun cancelledHeaderDiscoveryClosesItsOwnSpoolButDoesNotRemoveUnrelatedFiles()=runBlocking {
  val directory=java.nio.file.Files.createTempDirectory("qa-preamble-cancel-").toFile();val unrelated=File(directory,"user.txt").apply{writeText("USER")}
  var steps=0
  try {
   val result=runCatching {NativeEpubArchiveWriter.writeTransformedTxt(StringWriter(),StringReader("Alice ".repeat(15000)+"\nBody\nTail"),{_,t,b->NativeDownloadChapterText(t,b)},
       awaitIfPaused={if(++steps>=3)throw kotlinx.coroutines.CancellationException("synthetic cancel")},preambleDirectory=directory)}
   assertTrue(result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
   assertEquals(listOf("user.txt"),directory.listFiles()!!.map{it.name})
  }finally{unrelated.delete();directory.delete()}
 }
 @Test fun headerlessSourceAboveThePrefixBufferStillRunsOneWholeChapterRule()=runBlocking {
  val directory=java.nio.file.Files.createTempDirectory("qa-headerless-").toFile();val out=StringWriter();var calls=0
  val source="Alice\n"+"text\n".repeat(18000)+"Tail"
  try {
   NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader(source),{n,t,b->calls++;assertEquals(1,n);assertEquals("",t);assertEquals(source,b);NativeDownloadChapterText(t,b.replace("Alice","Bob"))},preambleDirectory=directory)
   assertEquals(1,calls);assertEquals(source.replace("Alice","Bob")+"\n",out.toString());assertTrue(directory.listFiles()!!.isEmpty())
  }finally{directory.delete()}
 }
 @Test fun leadingBlankBodyLinesAreNotDroppedDuringReplacementExport()=runBlocking {
  assertEquals("第1章\n\n\nBob\n",txt("第1章\n\n\nAlice"))
 }
 @Test fun genericTextMappingPreservesEntitiesAndProtectsMarkupInjection()=runBlocking {
  val out=StringWriter()
  NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader("第1章\n<p title='Alice'>Tom &amp; Jerry</p>"),{_,t,b->NativeDownloadChapterText(t,b,b,{it.replace("Tom & Jerry","<img src='fake'>")})})
  assertTrue(out.toString().contains("title='Alice'"));assertTrue(out.toString().contains("&lt;img src='fake'&gt;"));assertFalse(out.toString().contains(">Tom"))
 }
 @Test fun missingImageDoesNotCreateNestedParagraphTags()=runBlocking {
  val out=ByteArrayOutputStream()
  NativeEpubArchiveWriter.write(out,NativeEpubMetadata("QA","Synthetic"),StringReader("第1章\n[图片: https://fixture.invalid/missing.png]"),
     openAsset={NativeEpubAsset("image/png",byteArrayOf().inputStream())})
  var body="";ZipInputStream(out.toByteArray().inputStream()).use{z->while(true){val e=z.nextEntry ?: break;if(e.name=="OEBPS/chapter-1.xhtml")body=z.readBytes().toString(Charsets.UTF_8)}}
  val doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(body.byteInputStream())
  val p=doc.getElementsByTagName("p")
  for(index in 0 until p.length)assertEquals(0,(p.item(index) as org.w3c.dom.Element).getElementsByTagName("p").length)
  assertTrue(body.contains("image-missing"))
 }

 @Test fun txtPlainReplacementKeepsLiteralAngleBracketsWhileAttributesAreProtected()=runBlocking {
  val value=txt("第1章\nAlice","<em>literal</em> **stars**")
  assertTrue(value.contains("<em>literal</em> **stars**"));assertFalse(value.contains("&lt;"))
 }
 @Test fun epubAttributeMarkdownIsOpaqueBeforeFormattingOrRuleApplication()=runBlocking {
  val entries=epub("第1章\n<span title=\"**Alice** & more\">**Alice**</span>")
  val xml=entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
  assertTrue(xml.contains("title=&quot;**Alice** &amp; more&quot;"));assertTrue(xml.contains("<strong>Bob</strong>"))
  assertFalse(xml.contains("title=&quot;**Bob**"))
 }
 @Test fun epubPlainUrlAndImageAltMarkdownNeverReachTheReplacementOrStyleParser()=runBlocking {
  val entries=epub("第1章\nAlice https://fixture.invalid/**Alice**?q=Alice&x=1 ![**Alice**](https://fixture.invalid/Alice.png)")
  val xml=entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
  assertTrue(xml.contains("https://fixture.invalid/**Alice**?q=Alice&amp;x=1"));assertFalse(xml.contains("fixture.invalid/**Bob**"))
  assertTrue(xml.contains("![**Alice**](https://fixture.invalid/Alice.png)"));assertEquals(0,Regex("<img\\b").findAll(xml).count())
 }

 @Test fun headerlessMultilineRegexIsAppliedAcrossLinesNotResetForEachBufferedChunk()=runBlocking {
  val rule=ReaderReplacementRule("qa-multi",2,"Alice\\s+Body","Bob",isRegex=true)
  val snapshot=readerDownloadReplacementSnapshot(true,ReaderReplacementState(novelId=2,personalRules=listOf(rule)))
  val out=StringWriter();NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader("Alice\nBody"),{n,t,b->snapshot.transform(n,t,b).toNativeDownloadText()})
  assertEquals("Bob\n",out.toString())
 }
 @Test fun publicDisabledHiddenAndPersonalPriorityPoliciesRemainFrozenDuringExport()=runBlocking {
  val shared=ReaderReplacementRule("s",2,"Alice","PUBLIC",owner=ReaderReplacementOwner.Shared,sharedRuleId="shared")
  val hidden=ReaderReplacementRule("hidden",2,"Body","SHOULD_NOT_APPLY",owner=ReaderReplacementOwner.Shared,sharedRuleId="hidden")
  val personal=ReaderReplacementRule("p",2,"Alice","PERSONAL")
  for(enabled in listOf(false,true)) {
   val state=ReaderReplacementState(novelId=2,personalRules=listOf(personal),sharedRules=LoadResult.Success(listOf(shared,hidden)),
      hiddenSharedRuleIds=setOf("hidden"),sharedRulesEnabledOverride=enabled)
   val frozen=readerDownloadReplacementSnapshot(true,state);val out=StringWriter()
   NativeEpubArchiveWriter.writeTransformedTxt(out,StringReader("第1章\nAlice Body"),{n,t,b->frozen.transform(n,t,b).toNativeDownloadText()})
   assertEquals("第1章\nPERSONAL Body\n",out.toString())
  }
 }

 @Test fun txtKeepsRealLineBreaksWhenTheSourceContainsHtmlButNoReplacementLineBreakRule()=runBlocking {
  assertEquals("Bob\n<script>Alice</script>\nhttps://fixture.invalid/Alice\n",txt("Alice\n<script>Alice</script>\nhttps://fixture.invalid/Alice"))
 }
 private companion object {
  val PNG=byteArrayOf(0x89.toByte(),0x50,0x4e,0x47,13,10,26,10)+ByteArray(40)
 }
}
