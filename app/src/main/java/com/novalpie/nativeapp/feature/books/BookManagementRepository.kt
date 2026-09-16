package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.UploadFileSource
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.UploadDocument
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal interface BookManagementRepository {
    suspend fun info(id: Long): BookEditInfo
    suspend fun permissions(id: Long): BookEditPermissions
    suspend fun save(id: Long, request: BookEditRequest): BookEditResult
    suspend fun savePolicy(id: Long, policy: ManagedBookAccessPolicy): ForumActionResult
    suspend fun transfer(id: Long, identifier: String): ManagedBookTransferResult
    suspend fun uploadCover(id: Long, uri: String): String
}

/** API methods retain the strict confirmedManagedBookMutation boundary and original wire contracts. */
internal class WebsiteBookManagementRepository(
    private val api: NovalPieApi,
    private val readDocument: suspend (String) -> UploadDocument,
    private val source: (UploadDocument) -> UploadFileSource,
) : BookManagementRepository {
    override suspend fun info(id: Long) = api.managedBookInfo(id)
    override suspend fun permissions(id: Long) = api.managedBookPermissions(id)
    override suspend fun save(id: Long, request: BookEditRequest) = api.updateManagedBook(id, request)
    override suspend fun savePolicy(id: Long, policy: ManagedBookAccessPolicy) = api.updateManagedBookAccessPolicy(id, policy)
    override suspend fun transfer(id: Long, identifier: String) = api.transferManagedBook(id, identifier)
    override suspend fun uploadCover(id: Long, uri: String): String {
        val document = readDocument(uri)
        currentCoroutineContext().ensureActive()
        require(document.sizeBytes > 0L) { "封面文件为空" }
        require(document.mimeType?.startsWith("image/") == true) { "请选择图片文件" }
        return api.uploadManagedBookCover(id, source(document))
    }
}
