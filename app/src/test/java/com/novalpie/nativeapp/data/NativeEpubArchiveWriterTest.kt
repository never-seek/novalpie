package com.novalpie.nativeapp.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.ByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeEpubArchiveWriterTest {
    @Test
    fun ignoresDownloadMetadataPreambleBeforeTheFirstChapterHeading() = runBlocking {
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Preamble", author = "Writer"),
            source = StringReader("metadata\n\u603b\u7ae0\u8282\u6570: 1\n\n\u7b2c1\u7ae0\n\u6b63\u6587"),
            openAsset = { error("no image should be requested") },
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        val chapter = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
        val nav = entries.getValue("OEBPS/nav.xhtml").toString(Charsets.UTF_8)
        assertTrue(chapter.contains("\u6b63\u6587"))
        assertFalse(chapter.contains("metadata"))
        assertFalse(chapter.contains("\u603b\u7ae0\u8282\u6570"))
        assertEquals(1, "chapter-".toRegex().findAll(nav).count())
    }

    @Test
    fun dropsAMetadataPseudoChapterBeforeTheFirstRealChapter() = runBlocking {
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Preamble chapter", author = "Writer"),
            source = StringReader(
                "书名：Preamble chapter\n" +
                    "作者：Writer\n" +
                    "平台：novelPia\n" +
                    "生成时间：2026-08-23\n" +
                    "总章节数：1\n" +
                    "==================================================\n" +
                    "第1章\n" +
                    "书名：Preamble chapter\n" +
                    "正文",
            ),
            openAsset = { error("no image should be requested") },
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        val chapter = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
        assertTrue(chapter.contains("正文"))
        assertFalse(chapter.contains("平台：novelPia"))
        assertEquals(1, entries.keys.count { it == "OEBPS/chapter-1.xhtml" })
        assertFalse(entries.containsKey("OEBPS/chapter-2.xhtml"))
    }

    @Test
    fun keepsABackwardInternalHeadingInsideTheCurrentChapter() = runBlocking {
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Nested heading", author = "Writer"),
            source = StringReader(
                "第1章 第一\n第一章正文\n" +
                    "第2章 第二\n第二章正文\n第一章\n内部小标题\n" +
                    "第3章 第三\n第三章正文",
            ),
            openAsset = { error("no image should be requested") },
        )

        val chapters = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.startsWith("OEBPS/chapter-")) {
                    chapters[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }

        assertEquals(3, chapters.size)
        assertTrue(chapters.getValue("OEBPS/chapter-2.xhtml").contains("第一章"))
        assertTrue(chapters.getValue("OEBPS/chapter-2.xhtml").contains("内部小标题"))
        assertTrue(chapters.getValue("OEBPS/chapter-3.xhtml").contains("第3章 第三"))
    }

    @Test
    fun infersOpaqueImageTypeFromOriginalBytes() = runBlocking {
        val output = ByteArrayOutputStream()
        val pngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D,
        )

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Opaque image", author = "Writer"),
            source = StringReader("\u7b2c1\u7ae0\n[\u56fe\u7247: https://images.example.test/opaque.file]"),
            openAsset = {
                NativeEpubAsset(mediaType = "application/octet-stream", input = ByteArrayInputStream(pngBytes))
            },
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        assertArrayEquals(pngBytes, entries.getValue("OEBPS/images/image-1.png"))
        assertTrue(
            entries.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
                .contains("href=\"images/image-1.png\" media-type=\"image/png\"")
        )
    }

    @Test
    fun cleanupRemovesOnlyKnownNativeEpubTempFiles() {
        val directory = File.createTempFile("novalpie-cleanup-test-", ".dir").apply {
            delete()
            mkdirs()
        }
        val staleAsset = File(directory, "novalpie-asset-stale.bin").apply { writeBytes(byteArrayOf(1)) }
        val staleStage = File(directory, "novalpie-epub-stale.asset").apply { writeBytes(byteArrayOf(2)) }
        val unrelated = File(directory, "keep-me.bin").apply { writeBytes(byteArrayOf(3)) }

        try {
            assertEquals(2, cleanupNativeEpubTempFiles(directory))
            assertFalse(staleAsset.exists())
            assertFalse(staleStage.exists())
            assertTrue(unrelated.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cleanupRemovesStaleNativeEpubWorkDirectoriesButKeepsUnrelatedDirectories() {
        val root = File.createTempFile("novalpie-work-root-", ".dir").apply {
            delete()
            mkdirs()
        }
        val staleWork = File(root, "354491-123456789-987654321").apply {
            mkdirs()
            File(this, "novalpie-asset-stale.bin").writeBytes(byteArrayOf(1))
        }
        val unrelated = File(root, "keep-me").apply {
            mkdirs()
            File(this, "data.bin").writeBytes(byteArrayOf(2))
        }

        try {
            assertEquals(1, cleanupNativeEpubTempFiles(root))
            assertFalse(staleWork.exists())
            assertTrue(unrelated.exists())
            assertTrue(File(unrelated, "data.bin").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun stagedAssetCacheEvictsOnlyUnprotectedFilesAndDeletesThem() {
        fun staged(name: String, size: Long): NativeEpubStagedFile {
            val file = File.createTempFile("novalpie-cache-test-", name)
            file.writeBytes(ByteArray(size.toInt()))
            return NativeEpubStagedFile(file, "image/jpeg", size, 0L)
        }

        val first = staged("-a", 6L)
        val second = staged("-b", 6L)
        val third = staged("-c", 4L)
        val cache = NativeEpubStagedAssetCache(maxBytes = 10L)

        try {
            cache.put("a", first, protectedKeys = setOf("a"))
            cache.put("b", second, protectedKeys = setOf("a", "b"))
            assertTrue(first.file.exists())
            assertTrue(second.file.exists())

            cache.put("c", third)

            assertFalse(first.file.exists())
            assertTrue(second.file.exists())
            assertTrue(third.file.exists())
            assertEquals(10L, cache.sizeBytes)
        } finally {
            cache.clear()
            first.file.delete()
            second.file.delete()
            third.file.delete()
        }
    }

    @Test
    fun stagedAssetCacheRejectsAFileWhoseBytesWereTruncatedAfterStaging() {
        val file = File.createTempFile("novalpie-cache-integrity-", ".asset")
        file.writeBytes(ByteArray(10))
        val cache = NativeEpubStagedAssetCache(maxBytes = 100)
        cache.put(
            key = "https://images.example.test/truncated.jpg",
            value = NativeEpubStagedFile(
                file = file,
                mediaType = "image/jpeg",
                size = 10L,
                crc = 0L,
            ),
        )

        file.writeBytes(ByteArray(5))

        assertEquals(null, cache.get("https://images.example.test/truncated.jpg"))
        assertFalse(file.exists())
    }

    @Test
    fun consumesAssetInvokesCleanupAfterTheWriterCopiesIt() = runBlocking {
        val source = File.createTempFile("novalpie-source-cleanup-", ".asset")
        source.writeBytes(byteArrayOf(1, 2, 3, 4))
        val cleanupCalls = AtomicInteger(0)

        try {
            NativeEpubArchiveWriter.write(
                output = ByteArrayOutputStream(),
                metadata = NativeEpubMetadata(title = "Cleanup", author = "Writer"),
                source = StringReader("\u7b2c1\u7ae0\n\u6b63\u6587 [\u56fe\u7247: https://images.example.test/cleanup.jpg]"),
                openAsset = {
                    NativeEpubAsset(
                        mediaType = "image/jpeg",
                        input = source.inputStream(),
                        onConsumed = {
                            cleanupCalls.incrementAndGet()
                            source.delete()
                        },
                    )
                },
            )
            assertEquals(1, cleanupCalls.get())
            assertFalse(source.exists())
        } finally {
            source.delete()
        }
    }

    @Test
    fun stagesOriginalBytesWithStoredMetadataInOneRead() = runBlocking {
        val bytes = ByteArray(32_769) { index -> (index * 31).toByte() }
        val expectedCrc = CRC32().apply { update(bytes) }.value
        val destination = File.createTempFile("novalpie-stage-test-", ".asset")

        try {
            val staged = stageNativeEpubFile(
                input = ByteArrayInputStream(bytes),
                mediaType = "image/jpeg; charset=binary",
                destination = destination,
            )

            assertEquals(bytes.size.toLong(), staged.size)
            assertEquals(expectedCrc, staged.crc)
            assertEquals("image/jpeg", staged.mediaType)
            assertArrayEquals(bytes, staged.file.readBytes())
        } finally {
            destination.delete()
        }
    }

    @Test
    fun doesNotExposePartiallyStagedFileBeforeCopyCompletes() = runBlocking {
        val bytes = ByteArray(64 * 1024) { index -> (index * 17).toByte() }
        val destination = File.createTempFile("novalpie-stage-atomic-test-", ".asset").apply {
            delete()
        }
        var partialFileWasVisible = false
        val source = object : InputStream() {
            private var position = 0

            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (position >= bytes.size) return -1
                if (destination.isFile && destination.length() > 0L) {
                    partialFileWasVisible = true
                }
                val count = minOf(length, 4096, bytes.size - position)
                bytes.copyInto(buffer, offset, position, position + count)
                position += count
                return count
            }
        }

        try {
            val staged = stageNativeEpubFile(
                input = source,
                mediaType = "image/jpeg",
                destination = destination,
            )

            assertFalse("final staging path was visible before the copy completed", partialFileWasVisible)
            assertEquals(bytes.size.toLong(), staged.size)
            assertArrayEquals(bytes, destination.readBytes())
        } finally {
            destination.delete()
        }
    }

    @Test
    fun streamsChaptersKeepsOriginalImagesAndPreservesRepeatedSourceOccurrences() = runBlocking {
        val imageBytes = byteArrayOf(0, 1, 2, 127, -1, 42)
        val openedUrls = mutableListOf<String>()
        val progress = mutableListOf<NativeEpubExportProgress>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(
                title = "Native book",
                author = "Writer",
                description = "Description",
            ),
            source = StringReader(
                """
                第1章 开始
                第一段 [图片: https://images.example.test/shared.webp]
                第2章 继续
                第二段 [图片：https://images.example.test/shared.webp]
                """.trimIndent()
            ),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(
                    mediaType = "image/webp",
                    input = ByteArrayInputStream(imageBytes),
                )
            },
            onProgress = progress::add,
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        assertEquals("mimetype", entries.keys.first())
        assertEquals("application/epub+zip", entries.getValue("mimetype").toString(Charsets.US_ASCII))
        assertEquals(listOf("https://images.example.test/shared.webp"), openedUrls)
        assertArrayEquals(imageBytes, entries.getValue("OEBPS/images/image-1.webp"))
        assertArrayEquals(imageBytes, entries.getValue("OEBPS/images/image-2.webp"))
        assertTrue(entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8).contains("images/image-1.webp"))
        assertTrue(entries.getValue("OEBPS/chapter-2.xhtml").toString(Charsets.UTF_8).contains("images/image-2.webp"))
        val opf = entries.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
        assertTrue(opf.contains("image-1.webp"))
        assertTrue(opf.contains("image-2.webp"))
        assertTrue(opf.contains("chapter-2.xhtml"))
        assertEquals(2, progress.last().completedChapters)
        assertEquals(2, progress.last().completedImages)
    }

    @Test
    fun missingImageDoesNotDiscardCompletedChapterText() = runBlocking {
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Book", author = "Writer"),
            source = StringReader("第1章\n正文 [图片: https://images.example.test/missing.jpg]"),
            openAsset = { throw IllegalStateException("image unavailable") },
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        val chapter = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
        assertTrue(chapter.contains("正文"))
        assertTrue(chapter.contains("image-missing"))
    }

    @Test
    fun writesTheFullResolutionCoverAsAnIndependentEpubAsset() = runBlocking {
        val coverUrl = "https://images.example.test/layered-cover.webp"
        val imageBytes = byteArrayOf(9, 8, 7, 6)
        val openedUrls = mutableListOf<String>()
        val progress = mutableListOf<NativeEpubExportProgress>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(
                title = "Layered book",
                author = "Writer",
                coverUrl = coverUrl,
            ),
            source = StringReader("正文 [图片: $coverUrl]"),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(
                    mediaType = "image/webp",
                    input = ByteArrayInputStream(imageBytes),
                )
            },
            onProgress = progress::add,
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        assertEquals(listOf(coverUrl), openedUrls)
        assertArrayEquals(imageBytes, entries.getValue("OEBPS/images/cover.webp"))
        assertArrayEquals(imageBytes, entries.getValue("OEBPS/images/image-1.webp"))
        val coverPage = entries.getValue("OEBPS/cover.xhtml").toString(Charsets.UTF_8)
        assertTrue(coverPage.contains("src=\"images/cover.webp\""))
        val opf = entries.getValue("OEBPS/content.opf").toString(Charsets.UTF_8)
        assertTrue(opf.contains("id=\"cover-image\""))
        assertTrue(opf.contains("name=\"cover\" content=\"cover-image\""))
        assertTrue(opf.contains("id=\"cover-page\""))
        assertTrue(opf.contains("idref=\"cover-page\""))
        assertEquals(1, progress.last().totalImages)
        assertEquals(1, progress.last().completedImages)
    }

    @Test
    fun matchesWebsiteImageContractWithoutBundlingMarkdownOrHtmlImages() = runBlocking {
        val openedUrls = mutableListOf<String>()
        val progress = mutableListOf<NativeEpubExportProgress>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Website contract", author = "Writer"),
            source = StringReader(
                """
                第1章 正文
                [图片: https://images.example.test/bracket.jpg]
                ![markdown](https://images.example.test/markdown.jpg)
                <p><img src="https://images.example.test/html.jpg" /></p>
                """.trimIndent()
            ),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(
                    mediaType = "image/jpeg",
                    input = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                )
            },
            onProgress = progress::add,
        )

        val imageEntries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.startsWith("OEBPS/images/image-")) imageEntries += entry.name
            }
        }

        assertEquals(listOf("https://images.example.test/bracket.jpg"), openedUrls)
        assertEquals(listOf("OEBPS/images/image-1.jpg"), imageEntries)
        assertEquals(1, progress.last().completedImages)
    }

    @Test
    fun acceptsWebsiteWhitespaceAndFullWidthColonImagePlaceholders() = runBlocking {
        val openedUrls = mutableListOf<String>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Placeholder variants", author = "Writer"),
            source = StringReader(
                """
                第1章 正文
                [图片 https://images.example.test/space.jpg]
                [图片： https://images.example.test/full-width.jpg]
                """.trimIndent()
            ),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(
                    mediaType = "image/jpeg",
                    input = ByteArrayInputStream(byteArrayOf(4, 5, 6)),
                )
            },
        )

        assertEquals(
            listOf(
                "https://images.example.test/space.jpg",
                "https://images.example.test/full-width.jpg",
            ),
            openedUrls,
        )
    }

    @Test
    fun keepsEmptyWebsiteImagePlaceholderAsTextWithoutCreatingAnAsset() = runBlocking {
        val openedUrls = mutableListOf<String>()
        val progress = mutableListOf<NativeEpubExportProgress>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Empty placeholder", author = "Writer"),
            source = StringReader("第1章\n前文 [图片] 后文"),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(input = ByteArrayInputStream(byteArrayOf(1)))
            },
            onProgress = progress::add,
        )

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        val chapter = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
        assertEquals(emptyList<String>(), openedUrls)
        assertTrue(chapter.contains("[图片]"))
        assertEquals(0, progress.last().completedImages)
        assertFalse(chapter.contains("image-missing"))
    }

    @Test
    fun normalizesImageUrlsUsingWebsiteWhitespaceAndSlashRules() = runBlocking {
        val openedUrls = mutableListOf<String>()

        NativeEpubArchiveWriter.write(
            output = ByteArrayOutputStream(),
            metadata = NativeEpubMetadata(title = "URL contract", author = "Writer"),
            source = StringReader(
                "第1章\n[图片: /images/example image.jpg]",
            ),
            openAsset = { url ->
                openedUrls += url
                NativeEpubAsset(
                    mediaType = "image/jpeg",
                    input = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                )
            },
        )

        assertEquals(listOf("https://images/exampleimage.jpg"), openedUrls)
    }

    @Test
    fun usesStoredZipEntriesLikeTheWebsiteDefault() = runBlocking {
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Stored EPUB", author = "Writer"),
            source = StringReader("第1章\n正文"),
            openAsset = { error("no image should be requested") },
        )

        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            val methods = buildList {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    add(entry.method)
                }
            }
            assertTrue(methods.isNotEmpty())
            assertTrue(methods.all { it == ZipEntry.STORED })
        }
    }

    @Test
    fun retriesARepeatedUrlAfterATransientFailureLikeTheWebsiteGenerator() = runBlocking {
        val openedUrls = mutableListOf<String>()
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Retry repeated URL", author = "Writer"),
            source = StringReader(
                "第1章\n[图片: https://images.example.test/transient.jpg]\n" +
                    "第2章\n[图片: https://images.example.test/transient.jpg]",
            ),
            openAsset = { url ->
                openedUrls += url
                if (openedUrls.size == 1) throw IllegalStateException("temporary upstream failure")
                NativeEpubAsset(
                    mediaType = "image/jpeg",
                    input = ByteArrayInputStream(byteArrayOf(8, 9)),
                )
            },
        )

        val imageEntries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.startsWith("OEBPS/images/image-")) imageEntries += entry.name
            }
        }

        assertEquals(
            listOf(
                "https://images.example.test/transient.jpg",
                "https://images.example.test/transient.jpg",
            ),
            openedUrls,
        )
        assertEquals(
            listOf(
                "OEBPS/images/image-1.jpg",
                "OEBPS/images/image-2.jpg",
            ),
            imageEntries,
        )
    }

    @Test
    fun downloadsDifferentImageAssetsConcurrentlyWithoutChangingZipOrder() = runBlocking {
        val activeLoads = AtomicInteger(0)
        val maxActiveLoads = AtomicInteger(0)
        val imageUrls = listOf(
            "https://images.example.test/one.jpg",
            "https://images.example.test/two.jpg",
            "https://images.example.test/three.jpg",
        )
        val output = ByteArrayOutputStream()

        NativeEpubArchiveWriter.write(
            output = output,
            metadata = NativeEpubMetadata(title = "Concurrent EPUB", author = "Writer"),
            source = StringReader(
                "\u7b2c1\u7ae0\n" + imageUrls.joinToString("\n") { "[\u56fe\u7247: $it]" },
            ),
            imageConcurrency = 3,
            openAsset = { url ->
                val active = activeLoads.incrementAndGet()
                maxActiveLoads.updateAndGet { current -> maxOf(current, active) }
                try {
                    delay(50)
                    NativeEpubAsset(
                        mediaType = "image/jpeg",
                        input = ByteArrayInputStream(url.toByteArray()),
                    )
                } finally {
                    activeLoads.decrementAndGet()
                }
            },
        )

        val imageEntries = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.startsWith("OEBPS/images/image-")) {
                    imageEntries += entry.name to zip.readBytes()
                }
            }
        }

        assertTrue("expected more than one image request in flight", maxActiveLoads.get() > 1)
        assertTrue("image workers must honor the requested bound", maxActiveLoads.get() <= 3)
        assertEquals(
            listOf(
                "OEBPS/images/image-1.jpg",
                "OEBPS/images/image-2.jpg",
                "OEBPS/images/image-3.jpg",
            ),
            imageEntries.map { it.first },
        )
        assertEquals(imageUrls.map { it.toByteArray().toList() }, imageEntries.map { it.second.toList() })
    }
}
