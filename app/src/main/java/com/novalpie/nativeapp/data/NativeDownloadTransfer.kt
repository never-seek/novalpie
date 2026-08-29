package com.novalpie.nativeapp.data

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/**
 * Keeps a large native EPUB off the MediaStore/FUSE stream while it is being assembled.
 * Publication into Downloads happens only after the archive has been closed successfully.
 */
internal fun nativeEpubGenerationFile(workDirectory: File, bookId: Long): File {
    require(bookId > 0) { "书籍 ID 无效" }
    return File(workDirectory, "novalpie-$bookId.epub.part")
}

/** Copies a finished private download and fails closed if the byte count changes mid-copy. */
internal fun copyNativeDownloadFile(source: File, output: OutputStream): Long {
    if (!source.isFile) throw IOException("下载临时文件不存在")
    val expected = source.length()
    var copied = 0L
    source.inputStream().buffered(NATIVE_DOWNLOAD_COPY_BUFFER_BYTES).use { input ->
        val buffer = ByteArray(NATIVE_DOWNLOAD_COPY_BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            output.write(buffer, 0, read)
            copied += read
        }
    }
    output.flush()
    if (copied != expected) {
        throw IOException("下载文件复制不完整：预期 $expected 字节，实际 $copied 字节")
    }
    return copied
}

/**
 * Copies a finished private file while honoring the same cooperative pause gate used by the
 * network stream.  EPUB assembly happens in a private file first, but publishing that file to
 * MediaStore can still take minutes for a multi-gigabyte archive; keeping the checkpoint here
 * makes the visible Pause action cover the entire transfer rather than only archive generation.
 */
internal suspend fun copyNativeDownloadFilePausable(
    source: File,
    output: OutputStream,
    awaitIfPaused: suspend () -> Unit = {},
): Long {
    if (!source.isFile) throw IOException("下载临时文件不存在")
    val expected = source.length()
    val copied = source.inputStream().buffered(NATIVE_DOWNLOAD_COPY_BUFFER_BYTES).use { input ->
        copyNativeDownloadStream(
            input = input,
            output = output,
            awaitIfPaused = awaitIfPaused,
        )
    }
    if (copied != expected) {
        throw IOException("下载文件复制不完整：预期 $expected 字节，实际 $copied 字节")
    }
    return copied
}

/**
 * A cooperative pause gate for long native downloads.
 *
 * A socket read cannot be interrupted safely just because the user tapped Pause, so the current
 * read is allowed to finish and the next chunk/checkpoint waits here.  Cancellation remains a
 * separate operation and still closes the network call immediately through the API coroutine.
 */
internal class NativeDownloadControl {
    private val pausedState = MutableStateFlow(false)

    val isPaused: Boolean
        get() = pausedState.value

    fun pause() {
        pausedState.value = true
    }

    fun resume() {
        pausedState.value = false
    }

    suspend fun awaitIfPaused() {
        currentCoroutineContext().ensureActive()
        pausedState.first { paused -> !paused }
        currentCoroutineContext().ensureActive()
    }
}

/** Copies a stream in bounded memory and yields to the download pause gate between chunks. */
internal suspend fun copyNativeDownloadStream(
    input: InputStream,
    output: OutputStream,
    awaitIfPaused: suspend () -> Unit = {},
): Long {
    var copied = 0L
    input.use { source ->
        val buffer = ByteArray(NATIVE_DOWNLOAD_COPY_BUFFER_BYTES)
        while (true) {
            awaitIfPaused()
            val read = source.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            output.write(buffer, 0, read)
            copied += read
        }
    }
    output.flush()
    return copied
}

private const val NATIVE_DOWNLOAD_COPY_BUFFER_BYTES = 1024 * 1024

/**
 * Matches the filename contract used by the native download publisher:
 * `<safe title>_<book id>_<epoch millis>.(epub|txt)`.
 *
 * This deliberately stays stricter than a generic `.epub` check so MediaStore cleanup cannot
 * touch a user's unrelated pending download.
 */
internal fun isNativeDownloadDisplayName(value: String): Boolean =
    Regex("^.+_\\d+_\\d{10,17}\\.(?:epub|txt)$", RegexOption.IGNORE_CASE)
        .matches(value.trim())
