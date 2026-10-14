package com.novalpie.nativeapp.data
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35])
class CommentReactionBucketRegressionTest {
 @Test fun bookAndChapterRootsAndRepliesPreferTheirExplicitReactionBuckets()=runBlocking {
  MockWebServer().use{s->s.start();val body="""{"comments":[{"id":9,"helpful_count":13,"like_count":16,"not_helpful_count":0,"dislike_count":1,"funny_count":2,"reaction_count":7,"content":"root","replies":[{"id":10,"helpfulCount":0,"likeCount":8,"notHelpfulCount":4,"dislikeCount":6,"funnyCount":0,"reactionCount":9,"content":"reply"}]}]}"""
   repeat(2){s.enqueue(MockResponse().setBody(body))};val api=NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/'))
   for(comments in listOf(api.bookComments(2),api.chapterComments(2,10))){
    val root=comments.first{it.id==9L};assertEquals(16,root.likeCount);assertEquals(13,root.displayLikeCount);assertEquals(0,root.displayDislikeCount);assertEquals(2,root.displayReactionCount)
    val reply=comments.first{it.id==10L};assertEquals(0,reply.displayLikeCount);assertEquals(4,reply.displayDislikeCount);assertEquals(0,reply.displayReactionCount)
   }
  }
 }
 @Test fun aggregateAliasesRemainCompatibleWhenExplicitBucketsAreMissing()=runBlocking {
  MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("{\"comments\":[{\"id\":9,\"content\":\"QA\",\"like_count\":7,\"dislike_count\":3,\"reaction_count\":2}]}"))
   val c=NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/')).bookComments(2).single();assertEquals(7,c.likeCount);assertEquals(3,c.dislikeCount);assertEquals(2,c.reactionCount)
  }
 }

 @Test fun forumRepliesAlsoSeparateSpecificBucketsFromTheirLegacyAggregateFields()=runBlocking {
  MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("""{"comments":[{"id":9,"post_id":2,"content":"QA","helpful_count":0,"like_count":8,"not_helpful_count":4,"dislike_count":6,"funny_count":2,"reaction_count":7}]}"""))
   val c=NovalPieApi(baseUrl=s.url("/").toString().trimEnd('/')).forumPostComments(2).single()
   assertEquals(8,c.likeCount);assertEquals(0,c.displayLikeCount);assertEquals(4,c.displayDislikeCount);assertEquals(2,c.displayReactionCount)
  }
 }
}
