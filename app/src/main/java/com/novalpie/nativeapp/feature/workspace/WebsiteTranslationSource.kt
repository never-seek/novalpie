package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.data.NovalPieApi

internal class WebsiteTranslationSource(private val api: NovalPieApi) : TranslationSource {
    override suspend fun chapters(bookId: Long) = api.translationChapterCandidates(bookId)
    override suspend fun prepare(bookId: Long, chapterId: Long) = api.prepareWorkspaceTranslation(bookId, chapterId)
    override suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long) =
        api.submitWorkspaceTranslation(bookId, chapterId, content, title, result, model, elapsedMs)
}
