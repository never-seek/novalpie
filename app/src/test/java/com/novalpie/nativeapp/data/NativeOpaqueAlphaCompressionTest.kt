package com.novalpie.nativeapp.data

import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.StringReader
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** RGBA is not proof that any pixel is transparent. Real PNGs frequently carry unused alpha. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeOpaqueAlphaCompressionTest {
    @Test fun fullyOpaqueRgbaPngActuallyCompressesAtTheRequestedQuality() = runBlocking {
        val original = opaqueRgbaPng(1280, 1920)
        val decoded = checkNotNull(BitmapFactory.decodeByteArray(original,0,original.size))
        try { assertTrue("The regression fixture must carry an alpha channel",decoded.hasAlpha()); assertEquals(255,decoded.getPixel(17,19) ushr 24) } finally {decoded.recycle()}
        val low=File.createTempFile("rgba-quality-low-",".asset")
        val high=File.createTempFile("rgba-quality-high-",".asset")
        try {
            for((file,quality) in listOf(low to 25,high to 90)) {
                val result=stageNativeEpubFile(original.inputStream(),"image/png",file,compressImages=true,imageQuality=quality)
                assertEquals("Unused alpha must not silently disable compression", "image/jpeg",result.mediaType)
                val bitmap=checkNotNull(BitmapFactory.decodeFile(file.path))
                try {assertTrue(bitmap.width<=1080);assertTrue(bitmap.height<=1920);assertFalse(bitmap.hasAlpha())}finally{bitmap.recycle()}
            }
            assertTrue("Low quality must affect actual encoded bytes",low.length()<high.length())
            assertTrue(low.length()<original.size/2)
            println("RGBA_IMAGE_QUALITY original=${original.size} quality25=${low.length()} quality90=${high.length()}")
        }finally{low.delete();high.delete()}
    }

    @Test fun epubContainsReencodedPicturesRatherThanTheUnchangedRgbaSource() = runBlocking {
        val original=opaqueRgbaPng(900,1200)
        val outputs=listOf(false to 75,true to 25,true to 90).map{File.createTempFile("rgba-epub-",".epub")}
        try {
            for((index,setting) in listOf(false to 75,true to 25,true to 90).withIndex()) {
                outputs[index].outputStream().use { out->NativeEpubArchiveWriter.write(out,NativeEpubMetadata("RGBA压缩回归","合成测试"),StringReader("第1章 图片\n[图片: https://fixture.invalid/picture.png]"),
                    openAsset={NativeEpubAsset("image/png",original.inputStream())},compressImages=setting.first,imageQuality=setting.second,zipCompressionLevel=0) }
                ZipFile(outputs[index]).use{zip->
                    val image=zip.entries().asSequence().single{it.name.startsWith("OEBPS/images/")}
                    assertTrue(if(setting.first)image.name.endsWith(".jpg") else image.name.endsWith(".png"))
                    val data=zip.getInputStream(image).use{it.readBytes()}
                    if(!setting.first)assertArrayEquals(original,data) else assertFalse(original.contentEquals(data))
                }
            }
            assertTrue(outputs[1].length()<outputs[2].length());assertTrue(outputs[1].length()<outputs[0].length()/2)
        }finally{outputs.forEach(File::delete)}
    }

    @Test fun aRegionalDecoderThatCannotProveOpacityKeepsTheOriginal()=runBlocking {
        val original=opaqueRgbaPng(2200,4100)
        val target=File.createTempFile("rgba-large-opaque-",".asset")
        try{val result=stageNativeEpubFile(original.inputStream(),"image/png",target,compressImages=true,imageQuality=50)
            // Robolectric 4.14's ShadowBitmapRegionDecoder creates blank regions instead of
            // decoding source pixels. Fail closed here; real opaque + sparse-alpha positives
            // run through Android's actual decoder in ImageQualityCompressionDeviceTest.
            assertEquals("image/png",result.mediaType)
            assertArrayEquals(original,target.readBytes())
        }finally{target.delete()}
    }

    @Test fun aSingleTransparentOriginalPixelCannotBeLostByDownsampling()=runBlocking {
        val original=opaqueRgbaPng(2200,4100,transparentX=2199,transparentY=4099,noise=false)
        val target=File.createTempFile("rgba-sparse-alpha-",".asset")
        try{val result=stageNativeEpubFile(original.inputStream(),"image/png",target,compressImages=true,imageQuality=25)
            assertEquals("image/png",result.mediaType);assertArrayEquals(original,target.readBytes())
        }finally{target.delete()}
    }

    @Test fun disabledCompressionStillKeepsAnOpaqueRgbaPngByteExact()=runBlocking {
        val original=opaqueRgbaPng(80,100)
        val target=File.createTempFile("rgba-original-",".asset")
        try{stageNativeEpubFile(original.inputStream(),"image/png",target,compressImages=false);assertArrayEquals(original,target.readBytes())}finally{target.delete()}
    }
}

/** Deterministic valid PNG color type 6, even when every alpha sample is 255. */
internal fun opaqueRgbaPng(width:Int,height:Int,transparentX:Int=-1,transparentY:Int=-1,noise:Boolean=true,alphaLow:Int=255):ByteArray {
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
