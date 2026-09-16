package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.model.BookEditInfo
import com.novalpie.nativeapp.model.BookEditPermissions
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.BookAccessPolicyDraft
import com.novalpie.nativeapp.ui.BookEditDraft
import com.novalpie.nativeapp.ui.bookManagementActionsVisible

/** In-memory only. Credentials, URI contents and private drafts are never persisted here. */
data class BookManagementUiState(
    val bookId: Long = 0,
    val revision: Long = 0,
    val info: LoadResult<BookEditInfo> = LoadResult.Idle,
    val permissions: LoadResult<BookEditPermissions> = LoadResult.Idle,
    val draft: BookEditDraft = BookEditDraft(),
    val accessPolicyDraft: BookAccessPolicyDraft = BookAccessPolicyDraft(),
    val transferIdentifier: String = "",
    val saving: Boolean = false,
    val uploadingCover: Boolean = false,
    val savingAccessPolicy: Boolean = false,
    val transferringBook: Boolean = false,
    val actionMessage: String? = null,
) {
    val busy: Boolean get() = saving || uploadingCover || savingAccessPolicy || transferringBook
    // No new website permission is inferred: reuse the existing management-entry predicate.
    val canManage: Boolean get() = bookId > 0 && (info as? LoadResult.Success)?.value?.id == bookId &&
        bookManagementActionsVisible((permissions as? LoadResult.Success)?.value)
}
