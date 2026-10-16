package com.novalpie.nativeapp.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderCompletedUpdateBaselineTest {
    @Test fun readingSomeNewChaptersLocallyKeepsOnlyTheRemainingUpdateNotice() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = ReaderProgressStore(app)
        store.clear()
        store.save(9,9104,"旧章末",chapterNumber = 104,chapterCountAtLastRead = 104)
        store.save(9,9141,"继续阅读",chapterNumber = 141,chapterCountAtLastRead = 147)
        val entry = FavoriteEntry(book = NovelCard(9,"本机续读"),lastChapter = 141,chapterCount = 147)
        val card = compactFavoriteBookCardPresentation(entry,ReaderProgressStore(app).load(9))
        assertEquals("141/147",card.progressLabel)
        assertEquals("更新 6 章",card.updateLabel)
        store.save(9,9147,"最新章",chapterNumber = 147,chapterCountAtLastRead = 147)
        assertNull(compactFavoriteBookCardPresentation(entry.copy(lastChapter = 147),store.load(9)).updateLabel)
        assertEquals("更新 1 章",compactFavoriteBookCardPresentation(entry.copy(lastChapter = 147,chapterCount = 148),store.load(9)).updateLabel)
    }
    @Test fun legacyCompletedRecordIsReadWithoutResetAndSurvivesPartialContinuation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("novalpie_native_reader_progress",android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().putLong("book_9_chapter_id",9141).putInt("book_9_chapter_number",141)
            .putInt("book_9_chapter_count_at_last_read",104).apply()
        val store = ReaderProgressStore(app)
        assertEquals(104,store.load(9)?.completedCatalogueSize)
        store.save(9,9142,"继续",chapterNumber=142,chapterCountAtLastRead=147)
        assertEquals(104,ReaderProgressStore(app).load(9)?.completedCatalogueSize)
        val card=compactFavoriteBookCardPresentation(FavoriteEntry(book=NovelCard(9,"迁移"),lastChapter=142,chapterCount=147),store.load(9))
        assertEquals("更新 5 章",card.updateLabel)
    }
    @Test fun clearingOneBookRemovesItsCompletionEvidenceButPreservesOtherBooks() {
        val app=ApplicationProvider.getApplicationContext<Application>();val store=ReaderProgressStore(app);store.clear()
        store.save(9,9104,"完成",chapterNumber=104,chapterCountAtLastRead=104)
        store.save(10,10040,"其他书",chapterNumber=40,chapterCountAtLastRead=40)
        store.clear(9)
        assertNull(store.load(9));assertEquals(40,store.load(10)?.completedCatalogueSize)
        store.save(9,9001,"首次",chapterNumber=1,chapterCountAtLastRead=147)
        assertNull(store.load(9)?.completedCatalogueSize)
    }
    @Test fun anOrdinaryPausedBookDoesNotInventCompletionEvidenceOnRemoteProgress() {
        val app=ApplicationProvider.getApplicationContext<Application>();val store=ReaderProgressStore(app);store.clear()
        store.save(9,9070,"暂停",chapterNumber=70,chapterCountAtLastRead=147)
        val entry=FavoriteEntry(book=NovelCard(9,"未曾追平"),lastChapter=141,lastChapterId=9141,chapterCount=147)
        val merged=readerProgressAfterRemoteFavoriteProgress(store.load(9),entry)!!
        store.saveRemoteProgress(merged)
        assertNull(store.load(9)?.completedCatalogueSize)
        assertNull(compactFavoriteBookCardPresentation(entry,store.load(9)).updateLabel)
    }

    @Test fun aLegacyGlobalMirrorRetainsExplicitCompletionDuringAPartialRead() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("novalpie_native_reader_progress",android.content.Context.MODE_PRIVATE).edit().clear()
            .putLong("book_id",9).putLong("chapter_id",9141).putInt("chapter_number",141)
            .putInt("chapter_count_at_last_read",147).putInt("completed_catalogue_size",104).apply()
        assertEquals(104,ReaderProgressStore(app).load(9)?.completedCatalogueSize)
    }

}
