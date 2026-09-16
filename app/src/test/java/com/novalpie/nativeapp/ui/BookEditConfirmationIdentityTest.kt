package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import com.novalpie.nativeapp.model.*
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookEditConfirmationIdentityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aConfirmationOpenedForOneBookCannotConfirmTheNextBooksDraft() {
        fun state(id: Long) = BookEditState(bookId = id, revision = id,
            info = LoadResult.Success(BookEditInfo(id, "book$id", authorName = "author")),
            permissions = LoadResult.Success(BookEditPermissions(title = true, authorName = true)),
            draft = BookEditDraft(title = "book$id", authorName = "author"))
        var current by mutableStateOf(state(42))
        compose.setContent { MaterialTheme {
            BookEditInfoScreen(current, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText("保存基本信息"))
        compose.onNodeWithText("保存基本信息").performClick()
        compose.onNodeWithText("保存书籍信息").assertExists()
        compose.runOnIdle { current = state(43) }
        compose.onNodeWithText("保存书籍信息").assertDoesNotExist()
    }

    @Test fun anImagePickerOpenedForThePreviousBookCannotUploadToTheNextBook() {
        var code = 0
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                code = requestCode
            }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        fun state(id: Long) = BookEditState(bookId = id, revision = id,
            info = LoadResult.Success(BookEditInfo(id, "book$id", authorName = "author")),
            permissions = LoadResult.Success(BookEditPermissions(title = true, photoUrl = true)),
            draft = BookEditDraft(title = "book$id", authorName = "author"))
        var current by mutableStateOf(state(42))
        val uploaded = mutableListOf<Long>()
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            BookEditInfoScreen(current, {}, {}, { uploaded += current.bookId }, {}, {}, {}, {}, {})
        } } }
        compose.onNodeWithText("选择图片").performClick()
        assertTrue(code != 0)
        compose.runOnIdle { current = state(43) }
        compose.waitForIdle()
        compose.runOnIdle {
            registry.dispatchResult(code, Activity.RESULT_OK, Intent().setData(Uri.parse("content://fixture/cover.png")))
        }
        assertTrue("Late image selection must not upload to book43", uploaded.isEmpty())
    }

    @Test fun sameBookInfoCompletionMustNotDropAValidPickerResult() {
        var code = 0
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) { code = requestCode }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        var current by mutableStateOf(BookEditState(bookId = 42, revision = 1, info = LoadResult.Loading,
            permissions = LoadResult.Success(BookEditPermissions(photoUrl = true)),
            draft = BookEditDraft(title = "book", authorName = "author")))
        val selected = mutableListOf<String>()
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            BookEditInfoScreen(current, {}, {}, { selected += it }, {}, {}, {}, {}, {})
        } } }
        compose.onNodeWithText("选择图片").performClick()
        assertTrue(code != 0)
        compose.runOnIdle { current = current.copy(info = LoadResult.Success(BookEditInfo(42, "book", authorName = "author"))) }
        compose.waitForIdle()
        compose.runOnIdle { registry.dispatchResult(code, Activity.RESULT_OK, Intent().setData(Uri.parse("content://fixture/same-book.png"))) }
        org.junit.Assert.assertEquals(listOf("content://fixture/same-book.png"), selected)
    }
}
