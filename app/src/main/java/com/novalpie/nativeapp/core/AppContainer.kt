package com.novalpie.nativeapp.core

import android.content.Context
import android.webkit.CookieManager
import com.novalpie.nativeapp.data.AuthSessionStore
import com.novalpie.nativeapp.data.NetworkConfigStore
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.ProxySettings
import com.novalpie.nativeapp.data.SearchHistoryStore
import com.novalpie.nativeapp.data.SearchSettingsStore
import com.novalpie.nativeapp.data.isEmulatorRuntime
import com.novalpie.nativeapp.feature.search.SearchPreferences
import com.novalpie.nativeapp.feature.search.SearchRepository
import com.novalpie.nativeapp.feature.search.StoredSearchPreferences
import com.novalpie.nativeapp.feature.search.WebsiteSearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.novalpie.nativeapp.feature.reader.tts.AndroidSpeechEngine
import com.novalpie.nativeapp.feature.reader.tts.TtsPlaybackCoordinator
import com.novalpie.nativeapp.feature.reader.tts.WebsiteSpeechChapterSource
import com.novalpie.nativeapp.feature.reader.tts.SpeechChapter
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.feature.download.DownloadTask
import com.novalpie.nativeapp.feature.download.DownloadTaskStore
import com.novalpie.nativeapp.feature.download.DownloadCoordinator
import com.novalpie.nativeapp.feature.download.NativeDownloadTaskRunner
import kotlinx.coroutines.withContext
import java.io.File

/** Application-owned dependencies. Stores keep their Beta 6 names and serialization format. */
internal class AppContainer(context: Context) {
    private val application = context.applicationContext
    val environment = RequestEnvironment(AuthSessionStore(application), NetworkConfigStore(application))
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val uploadDrafts by lazy { com.novalpie.nativeapp.feature.upload.StoredUploadDrafts(
        com.novalpie.nativeapp.feature.upload.UploadDraftStore(File(application.noBackupFilesDir, "upload-drafts")), applicationScope) }
    val speechEngine by lazy { AndroidSpeechEngine(application,applicationScope) }
    val readingProgressSync by lazy {
        com.novalpie.nativeapp.feature.reader.progress.ReadingProgressSynchronizer(applicationScope, {environment.revision}) { bookId, chapterId ->
            api.saveReadingProgress(bookId, chapterId)
        }
    }
    private val speechProgress by lazy {com.novalpie.nativeapp.feature.reader.tts.SpeechProgressRecorder(application,api,applicationScope,readingProgressSync){environment.revision}}
    private val playbackDelegate = lazy { TtsPlaybackCoordinator(speechEngine,WebsiteSpeechChapterSource(application,api),applicationScope,speechProgress::record) }
    val playback by playbackDelegate
    fun refreshEnvironmentFromStores() {
        val revision=environment.revision
        environment.setToken(AuthSessionStore(application).loadToken())
        environment.setProxy(NetworkConfigStore(application).loadProxySettings())
        if(revision!=environment.revision) {
            if(playbackDelegate.isInitialized()){pendingSpeech=null;playback.stop()}
            if(downloadsDelegate.isInitialized()){pendingDownload=null;downloads.cancel()}
            if(translationsDelegate.isInitialized()){pendingTranslation=null;translations.environmentChanged()}
        }
    }
    // An in-process start payload avoids binder limits and never writes chapter prose to Intents.
    var pendingSpeech: PendingSpeech? = null
    var pendingDownload: DownloadTask? = null
    var pendingTranslation: com.novalpie.nativeapp.feature.workspace.TranslationTask? = null
    val translationStore by lazy { com.novalpie.nativeapp.feature.workspace.TranslationTaskStore(File(application.noBackupFilesDir, "translation-tasks")) }
    private val translationsDelegate = lazy {
        val modelClient = okhttp3.OkHttpClient.Builder().connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS).callTimeout(240, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).followRedirects(false).build()
        com.novalpie.nativeapp.feature.workspace.TranslationCoordinator(applicationScope, translationStore,
            com.novalpie.nativeapp.feature.workspace.TranslationRunner(com.novalpie.nativeapp.feature.workspace.WebsiteTranslationSource(api),
                com.novalpie.nativeapp.feature.workspace.CompatibleTranslationModel(modelClient,
                    { environment.proxy.toProxySelector(isEmulatorRuntime()) }), translationStore),
            { environment.token?.let { com.novalpie.nativeapp.data.decodeAuthTokenProfile(it)?.id } },
            { id -> com.novalpie.nativeapp.data.WorkspaceLocalStore(application).loadApis().firstOrNull { it.id == id } })
    }
    val translations by translationsDelegate
    val downloadStore by lazy {DownloadTaskStore(File(application.noBackupFilesDir,"download-tasks"))}
    private val downloadsDelegate=lazy {DownloadCoordinator(applicationScope,{task->withContext(Dispatchers.IO){downloadStore.save(task)}},NativeDownloadTaskRunner(application,api))}
    val downloads by downloadsDelegate
    data class PendingSpeech(val chapter: SpeechChapter,val settings: ReaderTtsSettings,val startIndex:Int)

    val api: NovalPieApi by lazy {
        NovalPieApi(
            authTokenProvider = { environment.token },
            // Only consulted by the existing rejected-auth fallback, never during cold startup.
            cookieProvider = {
                runCatching {
                    CookieManager.getInstance().getCookie("https://novalpie.cc")
                        ?.trim()?.takeIf(String::isNotEmpty)
                }.getOrNull()
            },
            proxySelectorProvider = { environment.proxy.toProxySelector(isEmulatorRuntime()) },
            requestRevisionProvider = { environment.revision },
        )
    }
    val searchRepository: SearchRepository by lazy { WebsiteSearchRepository(api) }
    val libraryRepository: com.novalpie.nativeapp.feature.library.LibraryRepository by lazy {
        com.novalpie.nativeapp.feature.library.WebsiteLibraryRepository(api)
    }
    val bookRepository: com.novalpie.nativeapp.feature.books.BookDetailRepository by lazy {
        com.novalpie.nativeapp.feature.books.WebsiteBookDetailRepository(api)
    }
    val messagesRepository: com.novalpie.nativeapp.feature.messages.MessagesRepository by lazy {
        com.novalpie.nativeapp.feature.messages.WebsiteMessagesRepository(api)
    }
    val replacementRemoteRepository: com.novalpie.nativeapp.feature.reader.replacement.ReplacementRemoteRepository by lazy {
        com.novalpie.nativeapp.feature.reader.replacement.WebsiteReplacementRemoteRepository(api)
    }
    val forumFeedRepository: com.novalpie.nativeapp.feature.forum.ForumFeedRepository by lazy {
        com.novalpie.nativeapp.feature.forum.WebsiteForumFeedRepository(api)
    }
    val forumPostRepository: com.novalpie.nativeapp.feature.forum.ForumPostRepository by lazy {
        com.novalpie.nativeapp.feature.forum.WebsiteForumPostRepository(api)
    }
    val blockingRepository: com.novalpie.nativeapp.feature.profile.BlockingRepository by lazy {
        com.novalpie.nativeapp.feature.profile.WebsiteBlockingRepository(api)
    }
    val profileRepository: com.novalpie.nativeapp.feature.profile.ProfileRepository by lazy {
        com.novalpie.nativeapp.feature.profile.WebsiteProfileRepository(api)
    }
    val publicProfileRepository: com.novalpie.nativeapp.feature.profile.PublicProfileRepository by lazy {
        com.novalpie.nativeapp.feature.profile.WebsitePublicProfileRepository(api)
    }
    val workspaceRepository: com.novalpie.nativeapp.feature.workspace.WorkspaceRepository by lazy {
        com.novalpie.nativeapp.feature.workspace.WebsiteWorkspaceRepository(api)
    }
    val adminRepository: com.novalpie.nativeapp.feature.admin.AdminRepository by lazy {
        com.novalpie.nativeapp.feature.admin.WebsiteAdminRepository(api)
    }
    val searchPreferences: SearchPreferences by lazy {
        StoredSearchPreferences(SearchSettingsStore(application), SearchHistoryStore(application))
    }

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun from(context: Context): AppContainer = instance ?: synchronized(this) {
            instance ?: AppContainer(context.applicationContext).also { instance = it }
        }
    }
}

/**
 * Session/proxy changes invalidate responses, not just connection pools. The observable stream
 * contains an opaque generation only; credentials cannot leak through state logging/toString.
 */
internal class RequestEnvironment(
    private val auth: AuthSessionStore,
    private val network: NetworkConfigStore,
) {
    @Volatile var token: String? = auth.loadToken()
        private set
    @Volatile var proxy: ProxySettings = network.loadProxySettings()
        private set
    private val changes = MutableStateFlow(0L)
    val revisions = changes.asStateFlow()
    val revision: Long get() = changes.value

    @Synchronized fun setToken(value: String?) {
        val normalized = value?.trim()?.takeIf(String::isNotEmpty)
        if (normalized == token) return
        if (normalized == null) auth.clearToken() else auth.saveToken(normalized)
        token = normalized
        changes.value++
    }

    @Synchronized fun setProxy(value: ProxySettings) {
        val normalized = value.copy(
            host = value.host.trim().ifBlank { ProxySettings.DEFAULT_PROXY_HOST },
            port = value.port.coerceIn(1, 65535),
        )
        if (normalized == proxy) return
        network.saveProxySettings(normalized)
        proxy = normalized
        changes.value++
    }
}
