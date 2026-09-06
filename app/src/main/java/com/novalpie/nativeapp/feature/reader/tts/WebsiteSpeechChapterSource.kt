package com.novalpie.nativeapp.feature.reader.tts

import android.content.Context
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.ReaderReplacementRulesStore
import com.novalpie.nativeapp.data.ReaderSettingsStore
import com.novalpie.nativeapp.feature.reader.text.DerivedTextPipeline
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import com.novalpie.nativeapp.ui.ReaderContentBlock
import com.novalpie.nativeapp.ui.ReaderReplacementState
import com.novalpie.nativeapp.ui.applyReaderReplacementRules
import com.novalpie.nativeapp.ui.mergeReaderReplacementPersonalRules
import com.novalpie.nativeapp.ui.readerBlocksForContent
import com.novalpie.nativeapp.ui.readerReplacementRulesForChapter
import com.novalpie.nativeapp.ui.readerTtsSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads real, source-authorized chapters for background continuation, with the same text pipeline. */
internal class WebsiteSpeechChapterSource(context: Context, private val api: NovalPieApi): SpeechChapterSource {
    private val application=context.applicationContext
    override suspend fun load(bookId: Long, chapterId: Long): SpeechChapter = withContext(Dispatchers.IO) {
        val chapters=api.chapters(bookId)
        val index=chapters.indexOfFirst { it.id==chapterId }
        require(index>=0){"听书章节不在当前目录中"}
        val settings=ReaderSettingsStore(application).load()
        val content=api.chapterContent(chapterId,settings.replaceMode,settings.showImages)
        val store=ReaderReplacementRulesStore(application)
        val local=store.loadPersonalRules(bookId)
        val personal=mergeReaderReplacementPersonalRules(local,api.personalGlossaries(bookId))
        val publicEnabled=store.loadSharedRulesEnabledOverride(bookId) ?: store.loadDefaultSharedRulesEnabled()
        val shared=if(publicEnabled)api.sharedGlossaries(bookId) else emptyList()
        val replacement=ReaderReplacementState(
            novelId=bookId,personalRules=personal,sharedRules=LoadResult.Success(shared),
            hiddenSharedRuleIds=store.loadHiddenSharedRuleIds(bookId),
            defaultSharedRulesEnabled=store.loadDefaultSharedRulesEnabled(),
            sharedRulesEnabledOverride=store.loadSharedRulesEnabledOverride(bookId),
        )
        val order=chapters[index].number ?: index+1
        val rules=readerReplacementRulesForChapter(replacement,order,ReaderReplacementTarget.Content)
        val text=DerivedTextPipeline.transform(content.content,rules,order)
        val titleRules=readerReplacementRulesForChapter(replacement,order,ReaderReplacementTarget.Title)
        val title=applyReaderReplacementRules(content.title ?: chapters[index].title,titleRules,order,ReaderReplacementTarget.Title).text
        buildSpeechChapter(bookId,chapterId,api.bookDetail(bookId).title,content.copy(title=title,content=text.markup),
            chapters.getOrNull(index+1)?.id,order,chapters.size,store.revision(bookId),settings.showImages,settings.removeDuplicateLines)
    }
}
