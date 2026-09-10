package com.novalpie.nativeapp.feature.upload

import com.novalpie.nativeapp.data.EpubParser
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.UploadFileSource
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Immutable identity of one user-confirmed write, including its already uploaded EPUB. */
internal data class UploadSubmission(val existingBookId: Long?, val request: UploadBookRequest, val document: UploadDocument)

internal interface UploadRepository {
    suspend fun document(uri: String): UploadDocument
    suspend fun parse(document: UploadDocument): ParsedEpub
    suspend fun submit(submission: UploadSubmission): UploadActionResult
    suspend fun submitTracked(submission: UploadSubmission, resume: UploadBatchCheckpoint?, checkpoint: suspend (UploadBatchCheckpoint) -> Unit): UploadActionResult = submit(submission)
}

internal class WebsiteUploadRepository(
    private val api: NovalPieApi,
    private val readDocument: suspend (String) -> UploadDocument,
    private val source: (UploadDocument) -> UploadFileSource,
) : UploadRepository {
    override suspend fun document(uri: String) = readDocument(uri)
    override suspend fun parse(document: UploadDocument): ParsedEpub {
        val input = source(document)
        return if (uploadParseMode(document.sizeBytes.coerceAtLeast(0)) == UploadParseMode.SERVER_CHUNKED) {
            val path = api.uploadFileInChunks(input)
            api.parseUploadedEpub(path).copy(epubFilePath = path)
        } else withContext(Dispatchers.IO) { EpubParser.parse(input) }
    }
    override suspend fun submit(submission: UploadSubmission): UploadActionResult {
        return submitTracked(submission, null) {}
    }
    override suspend fun submitTracked(submission: UploadSubmission, resume: UploadBatchCheckpoint?, checkpoint: suspend (UploadBatchCheckpoint) -> Unit): UploadActionResult {
        val request = submission.request
        val file = if (request.epubFilePath == null) source(submission.document) else null
        return submission.existingBookId?.let { id ->
            api.appendManagedChapters(id, request.submitType, request.chapters, request.epubFilePath, file, resume, checkpoint)
        } ?: api.uploadBook(request, epubFile = file, resume = resume, checkpoint = checkpoint)
    }
}
