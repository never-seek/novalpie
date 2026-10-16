package com.novalpie.nativeapp.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeTransparentImageOptimizationTest {
 @Test fun nonzeroAlphaPngCanShrinkWithoutLosingAnyRgbaPixel()=runBlocking {
  val original=opaqueRgbaPng(900,1200,noise=false,alphaLow=243)
  val file=File.createTempFile("qa-lossless-alpha-",".asset")
  try{
   val result=stageNativeEpubFile(original.inputStream(),"image/png",file,compressImages=true,imageQuality=25)
   assertEquals("image/webp",result.mediaType);assertTrue(file.length()<original.size)
   assertPixelsEqual(original,file.readBytes())
  }finally{file.delete()}
 }
 @Test fun nativeEpubUsesTheLosslessAlphaAssetWithTheRightExtensionAndPixels()=runBlocking {
  val original=opaqueRgbaPng(900,1200,noise=false,alphaLow=243)
  val file=File.createTempFile("qa-lossless-alpha-",".epub")
  try{
   file.outputStream().use{out->NativeEpubArchiveWriter.write(out,NativeEpubMetadata("透明图片无损优化","QA",coverUrl="https://fixture.invalid/alpha.png"),StringReader("第1章 验证\n透明层不能被抹掉。"),openAsset={NativeEpubAsset("image/png",original.inputStream())},compressImages=true,imageQuality=90,zipCompressionLevel=0)}
   ZipFile(file).use{zip->val entry=zip.entries().asSequence().single{it.name.startsWith("OEBPS/images/")};assertTrue(entry.name.endsWith(".webp"));val bytes=zip.getInputStream(entry).use{it.readBytes()};assertTrue(bytes.size<original.size);assertPixelsEqual(original,bytes);assertTrue(zip.getInputStream(zip.getEntry("OEBPS/content.opf")).use{it.reader().readText()}.contains("image/webp"))}
  }finally{file.delete()}
 }
 @Test fun compressionCannotReplaceASmallOriginalWithALargerJpeg()=runBlocking {
  val original=opaqueRgbaPng(1,1,noise=false)
  val file=File.createTempFile("qa-no-growth-",".asset")
  try{val result=stageNativeEpubFile(original.inputStream(),"image/png",file,compressImages=true,imageQuality=90);assertEquals("image/png",result.mediaType);assertArrayEquals(original,file.readBytes())}finally{file.delete()}
 }
 private fun assertPixelsEqual(original:ByteArray,optimized:ByteArray){
  fun decode(bytes:ByteArray)=checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inPreferredConfig=Bitmap.Config.ARGB_8888;inPremultiplied=false}))
  val a=decode(original);val b=decode(optimized)
  try{assertEquals(a.width,b.width);assertEquals(a.height,b.height);val left=IntArray(a.width);val right=IntArray(b.width);for(y in 0 until a.height){a.getPixels(left,0,a.width,0,y,a.width,1);b.getPixels(right,0,b.width,0,y,b.width,1);assertArrayEquals("RGBA pixels must stay exact at row $y",left,right)}}finally{a.recycle();b.recycle()}
 }
}
