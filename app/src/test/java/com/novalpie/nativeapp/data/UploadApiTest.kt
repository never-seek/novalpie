package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.UploadBookRequest
import com.novalpie.nativeapp.model.UploadChapter
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UploadApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun uploadBookUsesCurrentWebsiteMultipartFields() = runBlocking {
        server.enqueue(jsonResponse("""{"success":true,"novel_id":42,"message":"ok"}"""))
        val epub = uploadSource("sample.epub", "application/epub+zip", "epub-bytes".toByteArray())
        val request = UploadBookRequest(
            title = "Translated title",
            titleTranslation = "Original title",
            authorName = "Author",
            description = "Description",
            language = "ja",
            spans = "balanced",
            isAdult = true,
            source = "NovelPia",
            sourceUrl = "https://example.test/book/1",
            tags = listOf("fantasy", "romance"),
            submitType = "shared",
            chapters = listOf(UploadChapter("Chapter 1", "Body", 1))
        )

        val result = api.uploadBook(request, epubFile = epub)

        assertTrue(result.success)
        assertEquals(42L, result.novelId)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/uploads/books", recorded.requestUrl?.encodedPath)
        val contentType = recorded.getHeader("content-type").orEmpty()
        assertTrue(contentType.startsWith("multipart/form-data; boundary="))
        val body = recorded.body.readUtf8()
        listOf(
            "name=\"title\"", "Translated title",
            "name=\"title_translation\"", "Original title",
            "name=\"author_name\"", "Author",
            "name=\"language\"", "ja",
            "name=\"is_adult\"", "1",
            "name=\"tags\"", "fantasy,romance",
            "name=\"submit_type\"", "shared",
            "name=\"chapters\"", "Chapter 1",
            "name=\"chapters_md5\"",
            "name=\"epub_file\"; filename=\"sample.epub\"",
            "epub-bytes"
        ).forEach { expected -> assertTrue("Missing multipart value: $expected", body.contains(expected)) }
    }

    @Test
    fun largeEpubChunkUploadMatchesWebsiteFiveMiBProtocolWithoutWholeFileBuffer() = runBlocking {
        repeat(3) { server.enqueue(jsonResponse("""{"success":true}""")) }
        server.enqueue(jsonResponse("""{"success":true,"file_path":"uploads/fixed/sample.epub"}"""))
        val source = uploadSource("sample.epub", "application/epub+zip", "0123456789".toByteArray())

        val path = api.uploadFileInChunks(
            file = source,
            fileId = "fixed",
            chunkSizeBytes = 4
        )

        assertEquals("uploads/fixed/sample.epub", path)
        repeat(3) { index ->
            val chunk = server.takeRequest()
            assertEquals("POST", chunk.method)
            assertEquals("/api/uploads/chunks", chunk.requestUrl?.encodedPath)
            val body = chunk.body.readUtf8()
            assertTrue(body.contains("name=\"file_id\""))
            assertTrue(body.contains("fixed"))
            assertTrue(body.contains("name=\"chunk_index\""))
            assertTrue(body.contains(index.toString()))
            assertTrue(body.contains("name=\"total_chunks\""))
            assertTrue(body.contains("3"))
        }
        val merge = server.takeRequest()
        assertEquals("application/json", merge.getHeader("content-type")?.substringBefore(';'))
        JSONObject(merge.body.readUtf8()).also { body ->
            assertEquals("merge", body.getString("action"))
            assertEquals("fixed", body.getString("file_id"))
            assertEquals("sample.epub", body.getString("file_name"))
            assertEquals(3, body.getInt("total_chunks"))
        }
        Unit
    }

    @Test
    fun serverEpubParserUsesUploadedPathAndNormalizesMetadataAndChapters() = runBlocking {
        server.enqueue(jsonResponse("""
            {
              "success":true,
              "epub_file_path":"uploads/fixed/sample.epub",
              "metadata":{"title":"Novel","author":"Writer","description":"Intro","language":"ja"},
              "chapters":[
                {"title":"One","content":"Body 1","hierarchy_level":0,"section_path":["Part 1"],"raw_path":"one.xhtml","spine_index":0},
                {"title":"Two","content":"Body 2","hierarchy_level":1,"section_path":["Part 1"],"raw_path":"two.xhtml","spine_index":1}
              ]
            }
        """))

        val parsed = api.parseUploadedEpub("uploads/fixed/sample.epub")

        assertEquals("Novel", parsed.title)
        assertEquals("Writer", parsed.author)
        assertEquals("ja", parsed.language)
        assertEquals(2, parsed.chapters.size)
        assertEquals(2, parsed.chapters.last().chapterNumber)
        assertEquals(1, parsed.chapters.last().hierarchyLevel)
        assertEquals(listOf("Part 1"), parsed.chapters.last().sectionPath)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/uploads/epubs", recorded.requestUrl?.encodedPath)
        JSONObject(recorded.body.readUtf8()).also { body ->
            assertEquals("uploads/fixed/sample.epub", body.getString("file_path"))
            assertTrue(body.getBoolean("parse_only"))
        }
        Unit
    }

    private fun uploadSource(name: String, contentType: String, bytes: ByteArray) = UploadFileSource(
        fileName = name,
        sizeBytes = bytes.size.toLong(),
        contentType = contentType,
        openStream = { ByteArrayInputStream(bytes) }
    )

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(body.trimIndent())
}
