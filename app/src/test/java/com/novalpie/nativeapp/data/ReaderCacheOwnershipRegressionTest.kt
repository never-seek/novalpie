package com.novalpie.nativeapp.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.ChapterIllustration
import com.novalpie.nativeapp.model.ReaderContent
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class ReaderCacheOwnershipRegressionTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private fun token(id: Long, expiration: Long = 9999999999) = "header." +
        Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":$id,\"exp\":$expiration}".toByteArray()) + ".synthetic"
    @After fun cleanup() { AuthSessionStore(app).clearToken() }
    @Test fun switchingAccountCannotReadOrClearOtherAccountCache() {
        AuthSessionStore(app).saveToken(token(1))
        val a = ReaderChapterCacheStore(app)
        a.save(20, 30, "india", true, null, ReaderContent("A", "A_BODY", "fixture"))
        AuthSessionStore(app).saveToken(token(2))
        val b = ReaderChapterCacheStore(app)
        assertNull(b.load(20, 30, "india", true))
        b.save(20, 30, "india", true, null, ReaderContent("B", "B_BODY", "fixture"))
        b.clearBook(20)
        AuthSessionStore(app).saveToken(token(1))
        assertEquals("A_BODY", ReaderChapterCacheStore(app).load(20, 30, "india", true)?.content?.content)
    }
    @Test fun refreshedOrExpiredTokenOfSameAccountKeepsOfflineContent() {
        AuthSessionStore(app).saveToken(token(1))
        ReaderChapterCacheStore(app).save(20, 30, "india", true, null, ReaderContent("A", "OFFLINE", "fixture"))
        AuthSessionStore(app).saveToken(token(1, expiration = 1))
        assertEquals("OFFLINE", ReaderChapterCacheStore(app).load(20, 30, "india", true)?.content?.content)
    }
    @Test fun logoutDoesNotExposeAuthenticatedChapterToGuest() {
        AuthSessionStore(app).saveToken(token(1))
        ReaderChapterCacheStore(app).save(20, 30, "india", true, null, ReaderContent("A", "PRIVATE", "fixture"))
        AuthSessionStore(app).clearToken()
        assertNull(ReaderChapterCacheStore(app).load(20, 30, "india", true))
    }
    @Test fun cachedIllustrationRetainsSeparateOriginalAndAnimatedUrl() {
        val image = ChapterIllustration(7, 1, "https://fixture.invalid/outer.jpg", "https://fixture.invalid/inner.webp")
        val store = ReaderChapterCacheStore(app)
        store.save(20, 30, "india", true, null, ReaderContent("A", "body", "fixture", listOf(image)))
        assertEquals(image, store.load(20, 30, "india", true)?.content?.illustrations?.single())
    }
    @Test fun requestScopedWriterCannotWriteAnAResponseIntoB() {
        AuthSessionStore(app).saveToken(token(1))
        val aRequest = ReaderChapterCacheStore(app).forCurrentAccount()
        AuthSessionStore(app).saveToken(token(2))
        assertTrue(aRequest.save(20,30,"india",true,null,ReaderContent("A","A_RESPONSE","fixture")))
        assertNull(ReaderChapterCacheStore(app).load(20,30,"india",true))
        AuthSessionStore(app).saveToken(token(1))
        assertEquals("A_RESPONSE",ReaderChapterCacheStore(app).load(20,30,"india",true)?.content?.content)
    }
    @Test fun deniedEntryRemainsHiddenUntilAnAuthorizedSaveRestoresIt() {
        val store=ReaderChapterCacheStore(app)
        store.save(20,30,"india",true,null,ReaderContent("A","OLD","fixture"))
        store.revoke(20,30,"india",true)
        assertNull(store.load(20,30,"india",true))
        store.save(20,30,"india",true,null,ReaderContent("A","NEW","fixture"))
        assertEquals("NEW",store.load(20,30,"india",true)?.content?.content)
    }
    private fun legacyFile(): java.io.File = java.io.File(app.filesDir,"novalpie_reader_chapter_cache/20-30-india-images.json").also {
        it.parentFile!!.mkdirs()
        it.writeText("""{"book_id":20,"chapter_id":30,"replace_mode":"india","show_images":true,"content":"LEGACY","source":"fixture","title":"A"}""")
    }
    @Test fun retainedAccountKeepsPinnedLegacyCacheWithoutGivingItToNextLogin() {
        AuthSessionStore(app).saveToken(token(1))
        val a=ReaderChapterCacheStore(app)
        val original=legacyFile()
        assertEquals("LEGACY",a.load(20,30,"india",true)?.content?.content)
        AuthSessionStore(app).saveToken(token(2))
        assertNull(ReaderChapterCacheStore(app).load(20,30,"india",true))
        assertTrue(original.isFile)
    }
    @Test fun guestUpgradeNeverSilentlyAssignsUnownedLegacyCacheToLaterLogin() {
        AuthSessionStore(app).clearToken()
        ReaderChapterCacheStore(app)
        val original=legacyFile()
        AuthSessionStore(app).saveToken(token(1))
        assertNull(ReaderChapterCacheStore(app).load(20,30,"india",true))
        assertTrue(original.isFile)
    }
}
