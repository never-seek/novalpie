package com.novalpie.nativeapp.audit

import com.novalpie.nativeapp.feature.workspace.TranslationCoordinator
import com.novalpie.nativeapp.feature.workspace.TranslationService
import com.novalpie.nativeapp.feature.workspace.TranslationTask

/** Unexported debug-only dependency seam. Never compiled into the optimized Beta APK. */
class TranslationTestService : TranslationService() {
    override fun onCreate() { super.onCreate(); alive = true }
    override fun onDestroy() { super.onDestroy(); alive = false }
    internal override fun coordinator(): TranslationCoordinator = requireNotNull(queue)
    internal override fun refreshEnvironment() = Unit
    internal override fun takePendingTask(): TranslationTask? = pending.also { pending = null }

    companion object {
        @Volatile internal var alive = false
        internal var queue: TranslationCoordinator? = null
        internal var pending: TranslationTask? = null
    }
}
