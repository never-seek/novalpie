package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.download.*
import java.io.*
import java.net.ServerSocket
import java.net.Proxy
import java.util.UUID
import java.util.zip.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

/** Real Android codec, preferences and native-task EPUB output; no website points or novel text. */
class ImageQualityCompressionDeviceTest {
 @Test fun nativeTasksActuallyUseOffQuality25AndQuality90ForOpaqueRgbaPictures()=runBlocking {
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  val prefs=app.getSharedPreferences(DownloadSettingsStore.PREFERENCES_NAME,0);val before=prefs.all.toMap()
  val store=DownloadSettingsStore(app);val original=store.load()
  val png=deviceOpaqueRgbaPng(900,1200)
  val server=ServerSocket(0);val calls=AtomicInteger();val images=AtomicInteger()
  val base="http://127.0.0.1:${server.localPort}"
  val worker=thread(isDaemon=true,name="image-quality-native-http") {runCatching{
   while(!server.isClosed)server.accept().use{socket->
    val input=socket.getInputStream().bufferedReader();val path=input.readLine()?.split(" ")?.getOrNull(1)?.substringBefore('?').orEmpty()
    var length=0
    while(true){val line=input.readLine()?:break;if(line.isEmpty())break;if(line.startsWith("Content-Length:",true))length=line.substringAfter(':').trim().toInt()}
    repeat(length){input.read()}
    val response=when(path){
     "/api/novels/2/detail"->"""{"id":2,"title":"图片压缩实机验证","author":"合成测试","full_cover_url":"$base/picture.png"}""".toByteArray()
     "/api/downloads"->{calls.incrementAndGet();"""{"success":true,"file_name":"quality.txt"}""".toByteArray()}
     "/api/downloads/quality.txt"->"第1章 测试\n仅为原生图片压缩验证生成的合成正文。".toByteArray()
     "/picture.png"->{images.incrementAndGet();png}
     else->byteArrayOf()
    }
    val status=if(response.isEmpty())"404 Not Found" else "200 OK"
    val mime=if(path=="/picture.png")"image/png" else if(path=="/api/downloads/quality.txt")"text/plain" else "application/json"
    socket.getOutputStream().apply{write("HTTP/1.1 $status\r\nContent-Type: $mime; charset=utf-8\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray());write(response);flush()}
   }
  }}
  val outputs=mutableListOf<File>();val ids=mutableListOf<String>();val report=JSONArray()
  try {
   for((enabled,quality) in listOf(false to 75,true to 25,true to 90)) {
    store.save(original.copy(compressImages=enabled,imageQuality=quality,zipCompressionLevel=0))
    assertEquals(enabled,store.load().compressImages);assertEquals(quality,store.load().imageQuality)
    val id="qa-quality-"+UUID.randomUUID();ids+=id
    val output=File.createTempFile("qa-quality-$quality-",".epub",app.cacheDir);outputs+=output
    val api=NovalPieApi(client=OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build(),baseUrl=base)
    val result=NativeDownloadTaskRunner(app,api){_,file,_->file.copyTo(output,overwrite=true);"content://synthetic/$id"}
      .run(DownloadTask(id,1,2,"图片压缩实机验证",DownloadFormat.Epub),NativeDownloadControl()){}
    assertEquals(DownloadPhase.Completed,result.phase)
    ZipFile(output).use{zip->
      val image=zip.entries().asSequence().single{it.name.startsWith("OEBPS/images/")}
      val bytes=zip.getInputStream(image).use{it.readBytes()}
      if(enabled){assertTrue(image.name.endsWith(".jpg"));assertFalse(png.contentEquals(bytes))}else{assertTrue(image.name.endsWith(".png"));assertArrayEquals(png,bytes)}
      report.put(JSONObject().put("enabled",enabled).put("quality",quality).put("epubBytes",output.length()).put("imageBytes",bytes.size).put("imageEntry",image.name))
    }
   }
   assertTrue(outputs[1].length()<outputs[2].length());assertTrue(outputs[1].length()<outputs[0].length()/2)
   assertEquals(3,calls.get());assertEquals(3,images.get())
  } finally {
   server.close();worker.join(1000);outputs.forEach(File::delete)
   for(id in ids){val root=File(app.noBackupFilesDir,"download-work").canonicalFile;val dir=File(root,id).canonicalFile;check(dir.parentFile==root);dir.deleteRecursively()
     val journal=DownloadPublicationJournal(app,id,"runner").file;for(suffix in listOf("",".bak",".new"))File(journal.path+suffix).delete()}
   val edit=prefs.edit().clear();for((k,v) in before)when(v){is Int->edit.putInt(k,v);is Boolean->edit.putBoolean(k,v);is Long->edit.putLong(k,v);is Float->edit.putFloat(k,v);is String->edit.putString(k,v)};assertTrue(edit.commit());assertEquals(before,prefs.all)
  }
  File(app.getExternalFilesDir(null),"image-quality-native-verified.json").writeText(JSONObject().put("cases",report).put("zipLevel",0).put("settingsRestored",true).put("websiteRequests",0).toString(2))
 }

 @Test fun largeOpaqueRgbaCanCompressWithoutLosingTheSparseAlphaSafetyCheck()=runBlocking {
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  for(transparent in listOf(false,true)){
   val source=deviceOpaqueRgbaPng(2200,4100,transparentX=if(transparent)2199 else -1,transparentY=if(transparent)4099 else -1,noise=false)
   val target=File.createTempFile("qa-alpha-scan-",".asset",app.cacheDir)
   try{val result=stageNativeEpubFile(source.inputStream(),"image/png",target,compressImages=true,imageQuality=25)
     if(transparent){assertEquals("image/png",result.mediaType);assertArrayEquals(source,target.readBytes())}else assertEquals("image/jpeg",result.mediaType)
   }finally{target.delete()}
  }
 }

 @Test fun aNativeAlphaWebpEpubCanBeHandedToTheInstalledReader()=runBlocking {
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  val source=deviceOpaqueRgbaPng(900,1200,noise=false,alphaLow=243)
  val file=File.createTempFile("qa-alpha-compat-",".epub",app.cacheDir)
  val id="qa-alpha-compat-"+UUID.randomUUID()
  val task=DownloadTask(id,1,2,"透明图片压缩兼容性测试",DownloadFormat.Epub)
  val activity=androidx.test.core.app.ActivityScenario.launch(com.novalpie.nativeapp.audit.ReaderTestActivity::class.java)
  try{
   file.outputStream().use{out->NativeEpubArchiveWriter.write(out,NativeEpubMetadata("透明图片压缩兼容性测试","本地QA",coverUrl="https://fixture.invalid/test.png"),java.io.StringReader("第1章 测试\n这是合成颜色与透明度测试，不含小说正文。"),openAsset={NativeEpubAsset("image/png",source.inputStream())},compressImages=true,zipCompressionLevel=0)}
   ZipFile(file).use{zip->assertTrue(zip.entries().asSequence().any{it.name.endsWith(".webp")})}
   val uri=android.net.Uri.parse(AndroidDownloadPublisher(app).publish(task,file){})
   val intent=android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri,"application/epub+zip").setPackage("com.foobnix.pro.pdf.reader").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
   // Android 11 package visibility can hide a valid handler from queries; actual start is authoritative.
   app.startActivity(intent)
   File(app.getExternalFilesDir(null),"alpha-webp-compat.json").writeText(JSONObject().put("uri",uri.toString()).put("task",id).put("epubBytes",file.length()).put("reader","com.foobnix.pro.pdf.reader").toString(2))
  }finally{file.delete();activity.close()}
 }

 @Test fun realTransparentPngAndAnimatedWebpStillKeepTheirOriginalBytes()=runBlocking {
  val app=InstrumentationRegistry.getInstrumentation().targetContext
  for((mime,value) in listOf("image/png" to "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAf0lEQVR4nOXOMQEAIAzAsFIhaJpYBCJjR6Mg580dwiRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4iRO4twObPswNQHyWj+R0AAAAABJRU5ErkJggg==","image/webp" to "UklGRoQAAABXRUJQVlA4WAoAAAACAAAAPwAAPwAAQU5JTQYAAAAAAAAAAABBTk1GKAAAAAAAAAAAAD8AAD8AAJYAAAJWUDhMDwAAAC8/wA8ABxD9j/4HIqL/AQBBTk1GKAAAAAAAAAAAAD8AAD8AAJYAAABWUDhMDwAAAC8/wA8ABxDR//4HIqL/AQA=")){
   val source=android.util.Base64.decode(value,android.util.Base64.DEFAULT)
   val target=File.createTempFile("qa-protected-",".asset",app.cacheDir)
   try{val result=stageNativeEpubFile(source.inputStream(),mime,target,compressImages=true,imageQuality=1);assertEquals(mime,result.mediaType);assertArrayEquals(source,target.readBytes())}finally{target.delete()}
  }
 }
}

/** Deterministic valid PNG color type 6, even when every alpha sample is 255. */
private fun deviceOpaqueRgbaPng(width:Int,height:Int,transparentX:Int=-1,transparentY:Int=-1,noise:Boolean=true,alphaLow:Int=255):ByteArray {
    val data=ByteArrayOutputStream();val deflater=Deflater(1)
    try{DeflaterOutputStream(data,deflater).use{out->
        val row=ByteArray(width*4+1);var seed=17
        for(y in 0 until height){
            for(x in 0 until width){
                seed=seed*1664525+1013904223
                val offset=1+x*4
                row[offset]=if(noise)(seed ushr 16).toByte() else 120.toByte()
                row[offset+1]=if(noise)(seed ushr 8).toByte() else 160.toByte()
                row[offset+2]=if(noise)seed.toByte() else 200.toByte()
                row[offset+3]=when { x==transparentX&&y==transparentY -> 0;alphaLow<255 -> (alphaLow+((seed ushr 24)%(256-alphaLow))).toByte();else -> 255.toByte() }
            };out.write(row)
        }
    }}finally{deflater.end()}
    val file=ByteArrayOutputStream();val out=DataOutputStream(file)
    out.write(byteArrayOf(0x89.toByte(),80,78,71,13,10,26,10))
    fun chunk(name:String,body:ByteArray){val type=name.toByteArray(Charsets.US_ASCII);out.writeInt(body.size);out.write(type);out.write(body);val crc=CRC32();crc.update(type);crc.update(body);out.writeInt(crc.value.toInt())}
    val header=ByteArrayOutputStream().also{DataOutputStream(it).apply{writeInt(width);writeInt(height);write(byteArrayOf(8,6,0,0,0))}}.toByteArray()
    chunk("IHDR",header);chunk("IDAT",data.toByteArray());chunk("IEND",byteArrayOf());return file.toByteArray()
}
