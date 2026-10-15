package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.WorkspaceLocalStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/** Explicit live, read-only diagnostic: no keys/session values/novel text written to evidence. */
class LiveFeedbackDiagnosticsDeviceTest {
    @Test fun reportPersonalModelEligibilityAndRealFinalChapter()=runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveNovalPie")=="1")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val container=AppContainer.from(context).also{it.refreshEnvironmentFromStores()}
        val report=JSONObject().put("loggedIn",!container.environment.token.isNullOrBlank())
        val local=WorkspaceLocalStore(context).loadApis()
        report.put("localModels",JSONArray().apply{local.forEach{put(JSONObject()
            .put("sharedToServer",it.sharedToServer).put("hasServerId",it.serverId!=null)
            .put("configured",it.endpoint.isNotBlank()&&it.model.isNotBlank()&&it.apiKey.isNotBlank()))}})
        val remote=runCatching{container.api.workspaceApiConfigs()}
        report.put("remoteReadSuccess",remote.isSuccess)
        remote.getOrNull()?.let{models->report.put("remoteModels",JSONArray().apply{models.forEach{put(JSONObject()
            .put("active",it.isActive).put("healthy",it.isHealthy ?: JSONObject.NULL)
            .put("approval",it.approvalStatus ?: JSONObject.NULL).put("activation",it.activationStatus ?: JSONObject.NULL)
            .put("actual",it.actualStatus ?: JSONObject.NULL))}})}
        report.put("eligibleServerModels",remote.getOrNull().orEmpty().count{it.isActive&&it.isHealthy==true&&it.approvalStatus=="approved"})
        val chapters=container.api.chapters(360661)
        report.put("bookId",360661).put("chapterCount",chapters.size)
        chapters.lastOrNull()?.let{report.put("lastChapterId",it.id).put("lastChapterNumber",it.number ?: JSONObject.NULL)}
        val file=File(context.getExternalFilesDir(null),"feedback-readonly-diagnostic.json")
        file.writeText(report.toString(2));println("READONLY_FEEDBACK_DIAGNOSTIC "+report.toString())
    }

    @Test fun userAuthorizedOneChapterRetranslationAgainstActualWebsite()=runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveNovalPie")=="1"&&
            InstrumentationRegistry.getArguments().getString("allowLiveRetranslation")=="1")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val api=AppContainer.from(context).also{it.refreshEnvironmentFromStores()}.api
        val matches=api.searchPage("浪漫奇幻的NTR故事",scope="title",matchType="fuzzy_strict",limit=10).items
        val book=matches.single{it.title.replace(" ","").contains("浪漫奇幻的NTR故事",ignoreCase=true)}
        val chapters=api.chapters(book.id);val latest=chapters.maxBy{it.number ?: 0}
        val report=JSONObject().put("bookId",book.id).put("chapterCount",chapters.size)
            .put("lastChapterId",latest.id).put("lastChapterNumber",latest.number)
        // This is the one explicit live POST authorized by the user, never auto-replayed here.
        val submitted=runCatching {api.retranslateNovelChapters(book.id,listOf(requireNotNull(latest.number)))}
        report.put("requestSucceeded",submitted.isSuccess)
        submitted.getOrNull()?.let{report.put("accepted",it.success).put("message",it.message)}
        submitted.exceptionOrNull()?.let{report.put("errorClass",it.javaClass.simpleName)
            .put("status",(it as? com.novalpie.nativeapp.data.NovalPieApiException)?.statusCode ?: 0)}
        File(context.getExternalFilesDir(null),"feedback-live-retranslation.json").writeText(report.toString(2))
        println("LIVE_RETRANSLATION_RESULT "+report.toString())
    }
}
