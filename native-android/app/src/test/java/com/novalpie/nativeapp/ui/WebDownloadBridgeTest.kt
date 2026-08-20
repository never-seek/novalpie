package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDownloadBridgeTest {
    @Test
    fun completedDownloadKeyIsIdempotentButFailedKeyCanRetry() {
        var now = 1_000L
        val gate = WebDownloadIdempotencyGate(nowMillis = { now })

        assertTrue(gate.claim("blob-1"))
        assertFalse(gate.claim("blob-1"))
        assertFalse(gate.isCompleted("blob-1"))

        gate.release("blob-1")
        assertTrue(gate.claim("blob-1"))
        gate.complete("blob-1")
        assertTrue(gate.isCompleted("blob-1"))
        assertFalse(gate.claim("blob-1"))

        now += 10 * 60 * 1000L + 1L
        assertTrue(gate.claim("blob-1"))
    }

    @Test
    fun separateDownloadKeysDoNotBlockEachOther() {
        val gate = WebDownloadIdempotencyGate(nowMillis = { 10_000L })

        assertTrue(gate.claim("epub-a"))
        assertTrue(gate.claim("epub-b"))
        gate.complete("epub-a")
        gate.release("epub-b")

        assertTrue(gate.isCompleted("epub-a"))
        assertFalse(gate.claim("epub-a"))
        assertTrue(gate.claim("epub-b"))
    }

    @Test
    fun downloadFilenameAndMimeAreNormalizedWithoutChangingExistingExtensions() {
        assertEquals("book.epub", ensureWebDownloadExtension("book", "application/epub+zip"))
        assertEquals("book.txt", ensureWebDownloadExtension("book", "text/plain"))
        assertEquals("already.custom", ensureWebDownloadExtension("already.custom", "application/epub+zip"))
        assertEquals("unsafe_name_.epub", ensureWebDownloadExtension(
            sanitizeWebDownloadFilename(" unsafe/name?.epub "),
            "application/epub+zip",
        ))
    }

    @Test
    fun contentDispositionPrefersUtf8Filename() {
        assertEquals(
            "恋爱.epub",
            filenameFromContentDisposition(
                "attachment; filename=fallback.epub; filename*=UTF-8''%E6%81%8B%E7%88%B1.epub"
            )
        )
        assertEquals(
            "plain.txt",
            filenameFromContentDisposition("attachment; filename=plain.txt")
        )
    }

    @Test
    fun documentStartBridgeKeepsTheFirstPartyScopeAndBlobIdentityTransferKey() {
        assertEquals(setOf("https://novalpie.cc"), webDownloadDocumentStartOriginRules())
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("var blobKeys = new WeakMap();"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("var downloadPromises = new Map();"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("var completedKeys = new Set();"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("function downloadKeyFor(href, blob)"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("saveByChunks(blob, filename, mime, key)"))
    }

    @Test
    fun blobTransferUsesOneIdentityKeyWithRawBlobFirstAndChunkFallback() {
        // A repeated anchor click may expose two object URLs for one EPUB Blob. Both transports
        // must receive the same identity key so the native gate can create only one download. The
        // raw Blob POST stays first to avoid Base64 expansion for large EPUBs.
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("AndroidDownload.getBlobUploadUrl(filename, mime, key)"))
        val rawBlobPost = BLOB_DOWNLOAD_SCRIPT.indexOf("body: blob")
        val chunkFallback = BLOB_DOWNLOAD_SCRIPT.indexOf("await saveByChunks(blob, filename, mime, key);")
        assertTrue(rawBlobPost >= 0)
        assertTrue(chunkFallback > rawBlobPost)
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("id === '__NOVALPIE_DUPLICATE__'"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("id === '__NOVALPIE_IN_PROGRESS__'"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("Android download already in progress"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("if (completedKeys.has(key))"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("completedKeys.add(key);"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("return false;\n    } finally {"))
        assertFalse(BLOB_DOWNLOAD_SCRIPT.contains("JSZip"))
        assertFalse(BLOB_DOWNLOAD_SCRIPT.contains("imageConcurrency"))
    }

    @Test
    fun bridgeLifecycleCancelsOnlyActiveSessionsAndKeepsCompletedKeysIdempotent() {
        var now = 5_000L
        val gate = WebDownloadIdempotencyGate(nowMillis = { now })

        assertTrue(gate.claim("active-epub"))
        assertTrue(gate.claim("complete-epub"))
        gate.complete("complete-epub")

        // This models WebFallbackScreen leaving while an EPUB is still transferring. The active
        // key must become retryable, while the already-published file stays protected.
        gate.clearActive()

        assertTrue(gate.claim("active-epub"))
        assertFalse(gate.claim("complete-epub"))
    }

    @Test
    fun bridgeScriptNeverRebuildsAnEpubOrParsesIllustrationUrls() {
        assertFalse(BLOB_DOWNLOAD_SCRIPT.contains("[图片"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("body: blob"))
        assertTrue(BLOB_DOWNLOAD_SCRIPT.contains("blob.slice("))
    }
}
