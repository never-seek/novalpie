package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.ForumComment
import org.junit.Assert.*
import org.junit.Test

class ForumScopeRegressionTest {
    @Test fun richSpoilerIsItsOwnContainerEvenWithOutsideText() {
        val parsed=forumRichParagraphs("Outside ||inside [bookid:123] HIDDEN_TAIL|| end")
        assertEquals(3,parsed.size)
        assertTrue(parsed[1].segments.single() is ForumTextSegment.Spoiler)
        assertFalse(parsed[0].plainText.contains("HIDDEN_TAIL"))
        val link=forumRichParagraphs("||[name](https://fixture.invalid/inside)||").single().segments.single() as ForumTextSegment.Spoiler
        assertNotNull(link.richContent)
    }
    @Test fun unsafeHtmlCannotHideItsClosingTagInsideAnUnclosedFold() {
        assertTrue(forumBookReferenceIds("<script>[fold:bad][bookid:123]</script>").isEmpty())
        assertTrue(forumRichParagraphs("<style>[fold:bad]SHOULD_NOT_RENDER</style>").isEmpty())
    }
    @Test fun htmlCodeAndPreNeverActivateEmbeddedSharingMarkers() {
        val inline=forumRichParagraphs("<code>[bookid:123] &lt;div&gt;</code>").single()
        assertEquals(ForumTextSegment.InlineCode("[bookid:123] <div>"),inline.segments.single())
        assertTrue(forumBookReferenceIds("<code>[bookid:123]</code>").isEmpty())
        val block=forumRichParagraphs("<pre><code>&lt;div&gt;[bookid:123]&lt;/div&gt;</code></pre>").single().codeBlock!!
        assertEquals("<div>[bookid:123]</div>",block.value)
    }
    @Test fun spoilerContainingBookCardKeepsItsTailHidden() {
        val p=forumRichParagraphs("||before [bookid:123] SECRET_AFTER||")
        assertFalse(p.flatMap{it.segments}.any{it is ForumTextSegment.Plain&&it.value.contains("SECRET_AFTER")})
        assertTrue(p.flatMap{it.segments}.any{it is ForumTextSegment.Spoiler})
    }
    @Test fun boldSpoilerStillHasSpoilerIdentity() {
        assertTrue(forumRichParagraphs("**||SECRET||**").flatMap{it.segments}.any{it is ForumTextSegment.Spoiler})
    }
    @Test fun nestedFoldNeverMovesItsHiddenTailOutside() {
        val p=forumRichParagraphs("[fold:outer]before [fold:inner]inner[/fold] HIDDEN_TAIL[/fold]")
        assertEquals(1,p.size)
        assertTrue(p.single().fold!!.content.contains("HIDDEN_TAIL"))
        assertTrue(p.single().fold!!.content.contains("[/fold]"))
    }
    @Test fun fencedCodeInsideFoldRetainsExactSourceWithoutPlaceholders() {
        val outer=forumRichParagraphs("[fold:code]\n```html\n<div>[bookid:123] **literal**</div>\n```\n[/fold]").single().fold!!
        val code=forumRichParagraphs(outer.content).single().codeBlock!!
        assertEquals("<div>[bookid:123] **literal**</div>",code.value)
        assertTrue(forumBookReferenceIds(outer.content).isEmpty())
    }
    @Test fun inlineCodeAndEscapedBookExamplesAreNotCards() {
        assertTrue(forumBookReferenceIds("`[bookid:123]`").isEmpty())
        assertTrue(forumBookReferenceIds("\\[bookid:123]").isEmpty())
        assertTrue(forumBookReferenceIds("`![code](https://fixture.invalid/code.png)`").isEmpty())
    }
    @Test fun closedFoldsCannotProduceExternalLinkPreview() {
        assertTrue(forumCommentLinkPreviews(ForumComment(1,content="[fold:hidden][link](https://fixture.invalid/private)[/fold]")).isEmpty())
        assertTrue(forumContentLinks(listOf("[fold:hidden]https://fixture.invalid/private[/fold]")).isEmpty())
    }
    @Test fun nestedHtmlDetailsPreserveTheOuterBoundary() {
        val p=forumRichParagraphs("<details><summary>Outer</summary><p>before</p><details><summary>Inner</summary><p>SECRET</p></details><p>HIDDEN_TAIL</p></details>")
        assertEquals(1,p.size)
        val inner=forumRichParagraphs(p.single().fold!!.content)
        assertTrue(inner.any{it.fold?.title=="Inner"})
        assertTrue(inner.any{it.plainText=="HIDDEN_TAIL"})
    }
    @Test fun nestedFoldPreviewOnlyShowsItsClosedOuterTitle() {
        val s=forumFeedExcerptText("[fold:outer]before [fold:inner]inner[/fold] HIDDEN_TAIL[/fold]")
        assertFalse(s.contains("HIDDEN_TAIL"))
        assertEquals("outer",forumRichParagraphs(s).single().fold!!.title)
    }
    @Test fun ordinaryLinksBookWidgetsAndStylesStillWork() {
        assertEquals(listOf(123L),forumBookReferenceIds("**bold** [bookid:123]"))
        assertEquals(listOf("https://fixture.invalid/visible"),forumContentLinks(listOf("[visible](https://fixture.invalid/visible)")))
        assertTrue(forumRichParagraphs("**bold** *italic* ~~gone~~").single().segments.any{it is ForumTextSegment.Bold})
    }
}
