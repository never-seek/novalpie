package com.novalpie.nativeapp.ui

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.editor.AndroidEditorRepository
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.*

/** Actual Android parser/editor repository/archive/export, no website upload or private data reads. */
class EditorEpubResourceDeviceTest {
 private fun fixture():Pair<ByteArray,ByteArray> {
  val bitmap=Bitmap.createBitmap(4,3,Bitmap.Config.ARGB_8888).apply{eraseColor(0x402233ff)}
  val image=ByteArrayOutputStream().also{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray();bitmap.recycle()
  val out=ByteArrayOutputStream();ZipOutputStream(out).use{z->
   val files=mapOf("META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/content.opf'/></rootfiles></container>",
    "OPS/content.opf" to """<package xmlns='http://www.idpf.org/2007/opf'><metadata xmlns:dc='http://purl.org/dc/elements/1.1/'><dc:title>QA source</dc:title><dc:creator>QA author</dc:creator><dc:publisher>QA publisher</dc:publisher><dc:rights>QA rights</dc:rights></metadata><manifest><item id='c' href='text/c.xhtml' media-type='application/xhtml+xml'/><item id='i' href='images/a.png' media-type='image/png' properties='cover-image'/><item id='css' href='css/s.css' media-type='text/css'/></manifest><spine><itemref idref='c'/></spine></package>""",
    "OPS/text/c.xhtml" to "<html xmlns='http://www.w3.org/1999/xhtml'><head><link rel='stylesheet' href='../css/s.css'/></head><body><h1>QA chapter</h1><p>BEFORE Alice</p><img src='../images/a.png'/><p>MIDDLE</p><img src='../images/a.png'/><p>AFTER</p></body></html>",
    "OPS/css/s.css" to "img{max-width:100%;height:auto}")
   for((n,t) in files){z.putNextEntry(ZipEntry(n));z.write(t.toByteArray());z.closeEntry()}
   z.putNextEntry(ZipEntry("OPS/images/a.png"));z.write(image);z.closeEntry()
  };return out.toByteArray() to image
 }
 private fun validate(data:ByteArray,image:ByteArray,edited:Boolean) {
  val e=linkedMapOf<String,ByteArray>();ZipInputStream(data.inputStream()).use{z->while(true){val entry=z.nextEntry ?: break;e[entry.name]=z.readBytes()}}
  assertArrayEquals(image,e.filterKeys{it.endsWith(".png")}.values.single());assertEquals(1,e.keys.count{it.endsWith(".png")})
  val chapter=e.filterKeys{it.contains("novalpie-chapter-")}.values.single().toString(Charsets.UTF_8)
  val xml=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(chapter.byteInputStream())
  assertEquals(2,xml.getElementsByTagName("img").length)
  assertTrue(chapter.contains(if(edited)"EDITED"else "Alice"));assertTrue(chapter.indexOf("BEFORE")<chapter.indexOf("<img"));assertTrue(chapter.lastIndexOf("<img")<chapter.indexOf("AFTER"))
  val opf=e.getValue("OEBPS/content.opf").toString(Charsets.UTF_8);assertTrue(opf.contains("QA publisher"));assertTrue(opf.contains("QA rights"));assertTrue(opf.contains("cover-image"))
  assertTrue(e.keys.any{it.endsWith(".css")});assertFalse(chapter.contains("epub-node:"))
 }
 @Test fun realParserAndArchiveReloadPreserveEditedPicturesStylesAndMetadata() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  val (bytes,image)=fixture();val p=EpubParser.parse(UploadFileSource("qa.epub",bytes.size.toLong()){bytes.inputStream()},File(context.cacheDir,"beta7-epub-resource-qa"))
  val bundle=p.epubBundle!!;val store=EditorArchiveStore(context,"qa-epub-archive-${UUID.randomUUID()}")
  try {
   val metadata=EditorBookMetadata("Edited book",p.author,epubBundles=listOf(bundle))
   val original=ByteArrayOutputStream();EpubWriter.write(original,metadata,p.chapters);validate(original.toByteArray(),image,false)
   val edited=EditorProcessor.toWebsiteIdentifiers(p.chapters).replace("Alice","EDITED")
   store.save(EditorArchive("qa","QA",1,edited,metadata))
   val loaded=store.load("qa")!!;val chapters=EditorProcessor.parseWebsiteIdentifiers(loaded.textContent)
   val result=ByteArrayOutputStream();EpubWriter.write(result,loaded.metadata,chapters);validate(result.toByteArray(),image,true)
   File(context.cacheDir,"beta7-editor-epub-evidence").apply{mkdirs()}.let{dir->
    File(dir,"edited.epub").writeBytes(result.toByteArray())
    File(dir,"result.json").writeText(JSONObject().put("passed",true).put("images",1).put("occurrences",2).put("archiveRestored",true).put("remoteWrites",0).toString())
   }
  }finally{store.delete("qa");EpubResourceSupport.discard(bundle.id,File(bundle.archivePath))}
 }
 @Test fun actualEditorRepositoryPreparesAnOriginalQualityNativeUploadArchive()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
  val (bytes,image)=fixture();val doc=UploadDocument("content://qa/editor-fixture","qa.epub",bytes.size.toLong(),"application/epub+zip")
  val repo=AndroidEditorRepository(context,NovalPieApi(baseUrl="https://fixture.invalid"),{doc},{UploadFileSource("qa.epub",bytes.size.toLong()){bytes.inputStream()}})
  var prepared:UploadBookState?=null;var bundle:EpubDocumentBundle?=null
  try {
   val loaded=repo.loadDocument(doc.uri,"UTF-8",EditorBookMetadata());bundle=loaded.metadata.epubBundles.single()
   prepared=repo.prepareUpload(null,loaded.metadata.copy(title="Edited book"),loaded.chapters.map{it.copy(content=it.content.replace("Alice","EDITED"))})
   val uri=java.net.URI(prepared.selectedFile!!.uri);val file=File(uri);validate(file.readBytes(),image,true)
   assertTrue(prepared.actionMessage!!.contains("确认上传"))
  }finally{prepared?.let(repo::discardPrepared);bundle?.let{EpubResourceSupport.discard(it.id,File(it.archivePath))}}
 }
}
