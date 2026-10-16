package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.*
import java.io.File
import java.io.StringReader
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.novalpie.nativeapp.audit.ReaderTestActivity

/** Opt-in authorized image GETs only. No prose, signed URLs, keys or cookies are exported. */
class ImageQualityRealSamplesDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<ReaderTestActivity>()
 @Test fun compareActualWebsitePicturesWithCompressionOff25And90()=runBlocking {
  assumeTrue(InstrumentationRegistry.getArguments().getString("liveImageCompression")=="1")
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  compose.setContent{MaterialTheme{Text("正在验证图片压缩：读取原图并在本机对比，不修改书籍正文。")}}
  fun progress(stage:String){File(app.getExternalFilesDir(null),"image-quality-real-progress.json").writeText(JSONObject().put("stage",stage).put("time",System.currentTimeMillis()).toString())}
  progress("current-user")
  val cached=InstrumentationRegistry.getArguments().getString("verifiedRealFixtures")=="1"
  val expected=listOf(
   Triple("cover-source.jpg","book-353686-cover","36bd7a8ec9228c8868feabe04db2a04078daa322e5252fd03cd10eb8669a1b9b"),
   Triple("alpha-source-a.png","book-358153-chapter-11737633","1b2459ab51e1364d0bceaf339af192c6be75cdeba1e4fa24016f19efe287a937"),
   Triple("alpha-source-b.png","book-358153-chapter-11737633","e62285cc497b1bce4070251dcb85e3e7f999bbae1d32a42778b8d321ac330eee"))
  val fixtures=File(app.getExternalFilesDir(null),"qa-image-quality-originals")
  val images=mutableListOf<Pair<String,String>>()
  val api=AppContainer.from(app).also{it.refreshEnvironmentFromStores()}.api
  if(cached){
   for((name,label,sha) in expected){val f=File(fixtures,name);assertEquals(sha,fileSha(f));images+=label to f.path}
  }else{
   assertNotNull(api.currentUser().id)
   val cover=api.bookDetail(353686).let{it.fullCoverUrl?:it.coverUrl}
   if(!cover.isNullOrBlank())images+="book-353686-cover" to cover
   for(chapter in api.chapters(358153).takeLast(5).asReversed()){
    val content=api.chapterContent(chapter.id,showImages=true)
    for(image in readerBlocksForContent(content).filterIsInstance<ReaderContentBlock.Image>()){
     val url=image.originalUrl?:image.url
     if(images.none{it.second==url})images+="book-358153-chapter-${chapter.id}" to url
     if(images.size>=3)break
    };if(images.size>=3)break
   }
  }
  val root=File(app.cacheDir,"qa-real-image-quality-${System.currentTimeMillis()}").apply{mkdirs()}
  val cases=JSONArray();val errors=JSONArray();var compressedPng=0;var success=0
  try {
   for((index,pair) in images.take(3).withIndex()){
    progress("asset-$index")
    val source=File(root,"source-$index.bin");var declared:String?=null
    try {
     var error:Exception?=null
     for(attempt in 1..2){
      try{if(cached)File(pair.second).copyTo(source,overwrite=true) else source.outputStream().use{out->api.streamAsset(pair.second){input,type->declared=type;input.copyTo(out)}};error=null;break}
      catch(e:Exception){error=e}
     }
     if(error!=null)throw error
     val mime=inspectNativeEpubFileType(source,declared)
     val sample=JSONObject().put("sample",pair.first).put("originalBytes",source.length()).put("originalMime",mime).put("sourceMode",if(cached)"previous live GET bytes verified by SHA256" else "live GET")
     val originalPixels=if(mime=="image/png")pixelDigest(source.inputStream()) else null
     val row=JSONArray();val lengths=mutableListOf<Long>();val payloadSizes=mutableListOf<Long>()
     for((enabled,quality) in listOf(false to 75,true to 25,true to 90)){
      progress("encode-$index-$enabled-$quality")
      val output=File(root,"$index-$enabled-$quality.epub")
      output.outputStream().use{out->NativeEpubArchiveWriter.write(out,NativeEpubMetadata("图片压缩验证","本地QA",coverUrl="https://fixture.invalid/sample"),StringReader("第1章 验证\n仅用于图片编码比较的合成文字。"),
       openAsset={NativeEpubAsset(mime,source.inputStream())},compressImages=enabled,imageQuality=quality,zipCompressionLevel=0)}
      ZipFile(output).use{zip->
       val image=zip.entries().asSequence().single{it.name.startsWith("OEBPS/images/")}
       val sha=MessageDigest.getInstance("SHA-256");zip.getInputStream(image).use{input->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;sha.update(b,0,n)}}
       val exact=if(image.name.endsWith(".webp"))zip.getInputStream(image).use{pixelDigest(it)}==originalPixels else null
       if(exact!=null)assertTrue("Lossless alpha optimization must preserve every real source RGBA pixel",exact)
       row.put(JSONObject().put("rgbaPixelsExact",exact).put("enabled",enabled).put("quality",quality).put("epubBytes",output.length()).put("imageBytes",image.size).put("imageEntry",image.name).put("sha256",sha.digest().joinToString(""){"%02x".format(it)}))
       lengths+=output.length();payloadSizes+=image.size
      }
      output.delete()
     }
     sample.put("variants",row);cases.put(sample);success++
     if(mime=="image/png"&&row.getJSONObject(1).getString("imageEntry").endsWith(".webp")){
      compressedPng++;assertTrue(lengths[1]<lengths[0]);assertTrue(row.getJSONObject(1).getBoolean("rgbaPixelsExact"));assertTrue(row.getJSONObject(2).getBoolean("rgbaPixelsExact"))
     }
    }catch(e:Exception){errors.put(JSONObject().put("sample",pair.first).put("errorClass",e.javaClass.simpleName))}
    finally{source.delete()}
    File(app.getExternalFilesDir(null),"image-quality-real-samples.json").writeText(JSONObject().put("cases",cases).put("errors",errors).put("zipLevel",0).put("sourceUrlOrProseExported",false).toString(2))
   }
   assertTrue("Need actual successful source samples, not just a synthetic codec test",success>=2)
   assertTrue("Need actual alpha PNG optimization, not only a JPEG quality change",compressedPng>=1)
  }finally{
   check(root.parentFile==app.cacheDir&&root.name.startsWith("qa-real-image-quality-"));root.deleteRecursively()
  }
 }
 private fun fileSha(file:File):String=file.inputStream().use{input->val sha=MessageDigest.getInstance("SHA-256");val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;sha.update(b,0,n)};sha.digest().joinToString(""){"%02x".format(it)}}
 private fun pixelDigest(input:java.io.InputStream):String {
  val bitmap=input.use{android.graphics.BitmapFactory.decodeStream(it,null,android.graphics.BitmapFactory.Options().apply{inPremultiplied=false;inPreferredConfig=android.graphics.Bitmap.Config.ARGB_8888})}?:error("Actual picture failed to decode")
  try{val sha=MessageDigest.getInstance("SHA-256");val pixels=IntArray(bitmap.width);val bytes=ByteArray(bitmap.width*4)
   for(y in 0 until bitmap.height){bitmap.getPixels(pixels,0,bitmap.width,0,y,bitmap.width,1);for(x in pixels.indices){val value=pixels[x];bytes[x*4]=(value ushr 24).toByte();bytes[x*4+1]=(value ushr 16).toByte();bytes[x*4+2]=(value ushr 8).toByte();bytes[x*4+3]=value.toByte()};sha.update(bytes)}
   return sha.digest().joinToString(""){"%02x".format(it)}
  }finally{bitmap.recycle()}
 }

}
