package com.novalpie.nativeapp.data
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class EditorEpubArchiveRegressionTest {
 @Test fun savedAndReloadedMarkerTextRetainsItsPrivateResourceBundle(){
  val context=ApplicationProvider.getApplicationContext<Context>();val bytes=ByteArrayOutputStream()
  ZipOutputStream(bytes).use{z->
   for((name,text) in mapOf("META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/p.opf'/></rootfiles></container>",
      "OPS/p.opf" to "<package><metadata xmlns:dc='http://purl.org/dc/elements/1.1/'><dc:title>QA</dc:title><dc:creator>Author</dc:creator></metadata><manifest><item id='c' href='c.xhtml' media-type='application/xhtml+xml'/><item id='i' href='a.webp' media-type='image/webp'/></manifest><spine><itemref idref='c'/></spine></package>",
      "OPS/c.xhtml" to "<html><body><p>Before</p><img src='a.webp'/><p>After</p></body></html>")){
     z.putNextEntry(ZipEntry(name));z.write(text.toByteArray());z.closeEntry()}
   z.putNextEntry(ZipEntry("OPS/a.webp"));z.write(byteArrayOf(82,73,70,70,12,0,0,0,87,69,66,80));z.closeEntry()
  }
  val b=bytes.toByteArray();val parsed=EpubParser.parse(UploadFileSource("qa.epub",b.size.toLong()){b.inputStream()},java.io.File(context.filesDir,"qa-resources"))
  val metadata=EditorBookMetadata("QA","Author",epubBundles=listOf(parsed.epubBundle!!))
  val store=EditorArchiveStore(context,"qa-archive-${UUID.randomUUID()}")
  val manuscript=EditorProcessor.toWebsiteIdentifiers(parsed.chapters)
  store.save(EditorArchive("qa","QA",1,manuscript,metadata));val loaded=store.load("qa")!!
  assertEquals(metadata,loaded.metadata);assertEquals(manuscript,loaded.textContent)
  val chapters=EditorProcessor.parseWebsiteIdentifiers(loaded.textContent)
  val result=ByteArrayOutputStream();EpubWriter.write(result,loaded.metadata,chapters)
  val entries=linkedMapOf<String,ByteArray>();ZipInputStream(result.toByteArray().inputStream()).use{z->while(true){val e=z.nextEntry ?: break;entries[e.name]=z.readBytes()}}
  assertArrayEquals(byteArrayOf(82,73,70,70,12,0,0,0,87,69,66,80),entries.filterKeys{it.endsWith(".webp")}.values.single())
  assertTrue(entries.filterKeys{it.contains("novalpie-chapter-")}.values.single().toString(Charsets.UTF_8).contains("<img"))
  store.delete("qa");assertTrue(java.io.File(parsed.epubBundle!!.archivePath).isFile)
 }
}
