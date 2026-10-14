package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.zip.*
import javax.xml.parsers.DocumentBuilderFactory

class EpubResourceRoundTripRegressionTest {
 private val png=byteArrayOf(0x89.toByte(),0x50,0x4e,0x47,13,10,26,10)+ByteArray(40){it.toByte()}
 private val font="synthetic font bytes".toByteArray()
 private fun source():ByteArray {
  val out=ByteArrayOutputStream();ZipOutputStream(out).use {z->
   val text=linkedMapOf(
    "META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/content.opf'/></rootfiles></container>",
    "OPS/content.opf" to """<package xmlns='http://www.idpf.org/2007/opf' unique-identifier='id' version='3.0'><metadata xmlns:dc='http://purl.org/dc/elements/1.1/'><dc:identifier id='id'>urn:fixture</dc:identifier><dc:title>Original title</dc:title><dc:creator>QA author</dc:creator><dc:language>ko</dc:language><dc:description>Original description</dc:description><dc:publisher>QA publisher</dc:publisher><dc:date>2020-01-02</dc:date><dc:rights>QA rights</dc:rights><dc:subject>tag-one</dc:subject><meta name='cover' content='img'/></metadata><manifest><item id='c' href='text/c.xhtml' media-type='application/xhtml+xml'/><item id='img' href='pics/image.png' media-type='image/png' properties='cover-image'/><item id='css' href='css/style.css' media-type='text/css'/><item id='font' href='fonts/font.woff' media-type='font/woff'/></manifest><spine><itemref idref='c'/></spine></package>""",
    "OPS/text/c.xhtml" to """<html xmlns='http://www.w3.org/1999/xhtml'><head><title>Chapter</title><link rel='stylesheet' href='../css/style.css'/></head><body><h1>Chapter</h1><p>BEFORE Body</p><img src='../pics/image.png'/><p>MIDDLE</p><img src='../pics/image.png'/><p>AFTER</p></body></html>""",
    "OPS/css/style.css" to "@font-face{font-family:qa;src:url('../fonts/font.woff')} body{background-image:url('../pics/image.png')}"
   )
   for((n,t) in text){z.putNextEntry(ZipEntry(n));z.write(t.toByteArray());z.closeEntry()}
   for((n,b) in mapOf("OPS/pics/image.png" to png,"OPS/fonts/font.woff" to font)){z.putNextEntry(ZipEntry(n));z.write(b);z.closeEntry()}
  };return out.toByteArray()
 }
 private fun parsed():ParsedEpub {val b=source();return EpubParser.parse(UploadFileSource("qa.epub",b.size.toLong(),"application/epub+zip"){b.inputStream()})}
 private fun export(p:ParsedEpub,chapters:List<UploadChapter> = p.chapters):Map<String,ByteArray> {
  val out=ByteArrayOutputStream();EpubWriter.write(out,EditorBookMetadata(title="Edited title",author=p.author,language=p.language,description=p.description),chapters)
  val entries=linkedMapOf<String,ByteArray>();ZipInputStream(out.toByteArray().inputStream()).use{z->while(true){val e=z.nextEntry ?: break;entries[e.name]=z.readBytes()}}
  return entries
 }
 private fun chapter(entries:Map<String,ByteArray>)=entries.entries.filter{it.key.endsWith(".xhtml")&&it.key.contains("novalpie-chapter-")}.single{it.value.toString(Charsets.UTF_8).contains("BEFORE")}.value.toString(Charsets.UTF_8)
 @Test fun unchangedImportedChapterRetainsImageBytesPositionsAndRepeatedReferences(){
  val p=parsed();val e=export(p)
  val images=e.filterKeys{it.endsWith(".png")};assertEquals(1,images.size);assertArrayEquals(png,images.values.single())
  val body=chapter(e);val doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(body.byteInputStream())
  assertEquals(2,doc.getElementsByTagName("img").length)
  assertTrue(body.indexOf("BEFORE")<body.indexOf("<img"));assertTrue(body.indexOf("MIDDLE")>body.indexOf("<img"));assertTrue(body.lastIndexOf("<img")<body.indexOf("AFTER"))
 }
 @Test fun importedStylesFontsAndCoverMetadataSurviveWithoutImageReencoding(){
  val e=export(parsed());assertArrayEquals(font,e.filterKeys{it.endsWith(".woff")}.values.single())
  assertTrue(e.filterKeys{it.endsWith(".css")}.values.single().toString(Charsets.UTF_8).contains("../fonts/font.woff"))
  val opf=e.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
  assertTrue(opf.contains("cover-image"));assertTrue(opf.contains("image/png"));assertTrue(opf.contains("font/woff"))
 }
 @Test fun existingMetadataIsPreservedButTheUserEditedTitleIsApplied(){
  val e=export(parsed());val opf=e.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
  for(value in listOf("urn:fixture","QA publisher","2020-01-02","QA rights","tag-one","Edited title","QA author"))assertTrue("missing $value",opf.contains(value))
  assertFalse(opf.contains("2026-07-10T00:00:00Z"))
 }
 @Test fun editingPlainContentDoesNotDropEmbeddedImagesOrTheirRelativeOrder(){
  val p=parsed();val edited=p.chapters.map{it.copy(content=it.content.replace("Body","CHANGED"))};val e=export(p,edited)
  assertArrayEquals(png,e.filterKeys{it.endsWith(".png")}.values.single())
  val body=chapter(e);assertTrue(body.contains("CHANGED"));val doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(body.byteInputStream())
  assertEquals(2,doc.getElementsByTagName("img").length);assertTrue(body.indexOf("BEFORE")<body.indexOf("<img"));assertTrue(body.lastIndexOf("<img")<body.indexOf("AFTER"))
 }

 @Test fun markerTextReconstructionKeepsTheMediaEvenAfterDroppingPerChapterSourceFields(){
  val p=parsed();val rebuilt=EditorProcessor.parseWebsiteIdentifiers(EditorProcessor.toWebsiteIdentifiers(p.chapters))
  val metadata=EditorBookMetadata("New title",p.author,epubBundles=listOf(p.epubBundle!!))
  val out=ByteArrayOutputStream();EpubWriter.write(out,metadata,rebuilt.map{it.copy(content=it.content.replace("Body","EDITED"))})
  val e=linkedMapOf<String,ByteArray>();ZipInputStream(out.toByteArray().inputStream()).use{z->while(true){val entry=z.nextEntry ?: break;e[entry.name]=z.readBytes()}}
  val xml=chapter(e);assertTrue(xml.contains("EDITED"));assertEquals(2,DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream()).getElementsByTagName("img").length)
  assertArrayEquals(png,e.filterKeys{it.endsWith(".png")}.values.single())
 }
 @Test fun imageOnlyChapterIsNotDroppedAsEmpty(){
  val b=source();val original=linkedMapOf<String,ByteArray>();ZipInputStream(b.inputStream()).use{z->while(true){val e=z.nextEntry ?: break;original[e.name]=z.readBytes()}}
  original["OPS/text/c.xhtml"]="<html><body><img src='../pics/image.png'/></body></html>".toByteArray()
  val out=ByteArrayOutputStream();ZipOutputStream(out).use{z->original.forEach{(n,v)->z.putNextEntry(ZipEntry(n));z.write(v);z.closeEntry()}}
  val data=out.toByteArray();val p=EpubParser.parse(UploadFileSource("qa.epub",data.size.toLong()){data.inputStream()})
  assertEquals(1,p.chapters.size);assertTrue(p.chapters.single().content.contains("epub-node:"))
  assertTrue(export(p).keys.any{it.endsWith(".png")})
 }
 @Test fun corruptedCapturedResourceArchiveFailsRatherThanGeneratingABookMissingPictures(){
  val p=parsed();val file=File(p.epubBundle!!.archivePath);val bytes=file.readBytes();bytes[bytes.lastIndex]=(bytes.last().toInt() xor 0x40).toByte();file.writeBytes(bytes)
  assertTrue(runCatching{export(p)}.isFailure);assertTrue(file.isFile)
 }
 @Test fun alteredOriginalChapterMarkupCannotSilentlyOverrideTheEditedDocument(){
  val p=parsed();val bundle=p.epubBundle!!;val origin=bundle.origins.single()
  File(File(bundle.archivePath).parentFile,origin.markupFile).appendText("corrupt")
  assertTrue(runCatching{export(p)}.isFailure)
 }
 @Test fun missingResourceMarkerFailsWithTheManuscriptPreserved(){
  val p=parsed();val invalid=p.chapters.single().copy(content="[[epub-node:${p.epubBundle!!.id}:${"0".repeat(32)}]]")
  assertTrue(runCatching{export(p,listOf(invalid))}.isFailure);assertTrue(File(p.epubBundle!!.archivePath).isFile)
 }
 @Test fun generatedSpineListsOnlyEditedChaptersNotCopiedSourceHtmlFallbacks(){
  val e=export(parsed());val doc=DocumentBuilderFactory.newInstance().apply{isNamespaceAware=true}.newDocumentBuilder().parse(e.getValue("OEBPS/content.opf").inputStream())
  assertEquals(1,doc.getElementsByTagNameNS("*","itemref").length)
 }
}
