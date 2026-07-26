package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.EditorBookMetadata
import com.novalpie.nativeapp.model.UploadChapter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubWriterTest {
    @Test
    fun writesStandardEpubWithStoredMimetypeAndSpineChapters() {
        val output = ByteArrayOutputStream()

        EpubWriter.write(
            output = output,
            metadata = EditorBookMetadata(title = "Book", author = "Writer", language = "zh", description = "Intro"),
            chapters = listOf(
                UploadChapter("One", "Alpha & beta", 1),
                UploadChapter("Two", "Second body", 2)
            )
        )

        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        assertEquals("mimetype", entries.keys.first())
        assertEquals("application/epub+zip", entries["mimetype"])
        assertTrue(entries.containsKey("META-INF/container.xml"))
        assertTrue(entries["OEBPS/content.opf"].orEmpty().contains("chapter-2.xhtml"))
        assertTrue(entries["OEBPS/nav.xhtml"].orEmpty().contains("Two"))
        assertTrue(entries["OEBPS/chapter-1.xhtml"].orEmpty().contains("Alpha &amp; beta"))
    }
}
