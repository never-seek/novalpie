package com.novalpie.nativeapp.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.MessageActionResult
import com.novalpie.nativeapp.data.NovalPieApiException
import com.novalpie.nativeapp.ui.apiFailureMessage
import kotlinx.coroutines.*
import java.io.IOException

/** Each screen owns child jobs; disposing it cannot cancel another feature or an injected scope. */
internal abstract class MessageFeature(scope: CoroutineScope?) : ViewModel() {
    private val parent = scope ?: viewModelScope
    protected val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    protected var environment = 0L
        private set

    protected fun invalidateEnvironment() { environment++; work.coroutineContext.cancelChildren() }
    fun close() { environment++; work.cancel() }
    override fun onCleared() { close(); super.onCleared() }
}

internal suspend fun <T> messageAttempt(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}

internal fun MessageActionResult.requireAcknowledged(): MessageActionResult {
    check(success) { message?.takeIf(String::isNotBlank) ?: "服务器未接受此操作" }
    return this
}

internal fun <T> Result<T>.messageLoadResult(label: String): LoadResult<T> = fold(
    onSuccess = { LoadResult.Success(it) },
    onFailure = { LoadResult.Error(apiFailureMessage(label, it)) },
)

internal fun messageWriteFailure(label: String, failure: Throwable): String =
    if (failure is IOException && failure !is NovalPieApiException) "${label}结果未确认，请先刷新确认后再重试；草稿已保留"
    else apiFailureMessage(label, failure)
