package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ForumPreviewProtocolRegressionTest {
    @Test fun htmlDetailsSurvivesFeedApiNormalizationAndRemainsClosed() = runBlocking {
        MockWebServer().use {s->s.start();s.enqueue(MockResponse().setBody("""{"posts":[{"id":9,"title":"QA","type":"discussion","content":"<details><summary>Closed</summary><p>HIDDEN_MARK</p></details>"}]}"""))
            val post=NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/')).forumPostsPage().posts.single()
            val displayed=forumPostFeedItem(post).excerpt.orEmpty()
            assertEquals("Closed",forumRichParagraphs(displayed).single().fold!!.title)
            assertFalse(forumFeedExcerptText(displayed).contains("HIDDEN_MARK"))
        }
    }
}
