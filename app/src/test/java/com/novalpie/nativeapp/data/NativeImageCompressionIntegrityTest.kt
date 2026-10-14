package com.novalpie.nativeapp.data

import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeImageCompressionIntegrityTest {
    private fun bytes(name: String)=checkNotNull(javaClass.classLoader!!.getResourceAsStream("download-integrity/$name")).use {it.readBytes()}
    @Test fun transparentPngRemainsByteExactWithCompressionEnabled() = runBlocking {
        val original=bytes("transparent.png");val target=File.createTempFile("alpha-qa", ".asset")
        try { val result=stageNativeEpubFile(original.inputStream(),"image/png",target,compressImages=true)
            assertEquals("image/png",result.mediaType);assertArrayEquals(original,target.readBytes())
            val bitmap=checkNotNull(BitmapFactory.decodeFile(target.absolutePath))
            try {assertTrue(Color.alpha(bitmap.getPixel(0,0))<255)} finally {bitmap.recycle()}
        } finally {target.delete()}
    }
    @Test fun animatedWebpRemainsByteExactWithCompressionEnabled() = runBlocking {
        val original=bytes("animated.webp");val target=File.createTempFile("animation-qa", ".asset")
        try {val result=stageNativeEpubFile(original.inputStream(),"image/webp",target,compressImages=true)
            assertEquals("image/webp",result.mediaType);assertArrayEquals(original,target.readBytes())
        } finally {target.delete()}
    }
    @Test fun opaqueJpegStillCompressesAndUsesSelectedQuality() = runBlocking {
        val original=bytes("opaque.jpg");val target=File.createTempFile("jpeg-qa", ".asset")
        try {val result=stageNativeEpubFile(original.inputStream(),"image/jpeg",target,compressImages=true,imageQuality=60)
            assertEquals("image/jpeg",result.mediaType);assertFalse(original.contentEquals(target.readBytes()))
            val bitmap=checkNotNull(BitmapFactory.decodeFile(target.absolutePath))
            try {assertEquals(1080,bitmap.width);assertTrue(bitmap.height<=1920)} finally {bitmap.recycle()}
        } finally {target.delete()}
    }
    @Test fun disabledCompressionKeepsOpaqueJpegByteExact() = runBlocking {
        val original=bytes("opaque.jpg");val target=File.createTempFile("original-qa", ".asset")
        try {stageNativeEpubFile(original.inputStream(),"image/jpeg",target,compressImages=false)
            assertArrayEquals(original,target.readBytes())
        } finally {target.delete()}
    }
}
