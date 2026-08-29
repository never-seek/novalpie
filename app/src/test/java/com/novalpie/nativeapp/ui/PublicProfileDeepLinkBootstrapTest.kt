package com.novalpie.nativeapp.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.LoadResult
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class PublicProfileDeepLinkBootstrapTest {

    @Test
    fun publicProfileDeepLinkInitializesTheProfileRootNeededOnBack() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = NovalPieViewModel(application)

        viewModel.openDeepLink("novalpie://app/user/999999999")

        assertTrue(
            "Returning from a public profile must have a real owner-profile request in flight.",
            viewModel.profileState.profile is LoadResult.Loading,
        )
    }

    @Test
    fun authDeepLinkInitializesTheProfileRootNeededAfterCaptchaCancel() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = NovalPieViewModel(application)

        viewModel.openDeepLink("novalpie://app/login")

        assertTrue(
            "Returning from an auth/captcha flow must have a profile request in flight.",
            viewModel.profileState.profile is LoadResult.Loading,
        )
    }
}
