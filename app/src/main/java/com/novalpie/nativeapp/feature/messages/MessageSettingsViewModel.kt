package com.novalpie.nativeapp.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.apiFailureMessage
import com.novalpie.nativeapp.ui.validateMessageSettings
import kotlinx.coroutines.*

internal class MessageSettingsViewModel(
    private val repository: MessagesRepository,
    scope: CoroutineScope? = null,
) : MessageFeature(scope) {
    private var revision = 0L
    private var editRevision = 0L
    private var draftDirty = false
    private var readJob: Job? = null
    var state by mutableStateOf(MessageSettingsState())
        private set

    fun load() {
        if (state.saving) return
        val serial = ++revision
        val account = environment
        val edits = editRevision
        val preserveDraft = draftDirty
        readJob?.cancel()
        state = state.copy(settings = LoadResult.Loading, actionMessage = null)
        readJob = work.launch {
            val result = messageAttempt { repository.settings() }
            if (account != environment || serial != revision) return@launch
            state = result.fold(
                { state.copy(settings = LoadResult.Success(it), draft = if (preserveDraft || edits != editRevision) state.draft else it) },
                { state.copy(settings = LoadResult.Error(apiFailureMessage("消息设置", it))) },
            )
        }
    }

    fun edit(transform: (MessageSettings) -> MessageSettings) {
        editRevision++
        draftDirty = true
        state = state.copy(draft = transform(state.draft), actionMessage = null)
    }

    fun save() {
        if (state.saving || state.settings !is LoadResult.Success) return
        validateMessageSettings(state.draft)?.let { state = state.copy(actionMessage = it); return }
        val account = environment
        val submitted = state.draft.copy(notificationTypes = state.draft.notificationTypes?.toSet())
        val edits = editRevision
        state = state.copy(saving = true, actionMessage = null)
        work.launch {
            val result = messageAttempt { repository.saveSettings(submitted).requireAcknowledged() }
            if (account != environment) return@launch
            if (result.isSuccess) draftDirty = edits != editRevision
            state = result.fold(
                { state.copy(settings = LoadResult.Success(submitted), saving = false,
                    actionMessage = if (edits == editRevision) "消息设置已保存" else "已保存本次设置，后续修改尚未保存") },
                { state.copy(saving = false, actionMessage = messageWriteFailure("保存消息设置", it)) },
            )
        }
    }

    fun environmentChanged() { invalidateEnvironment(); revision++; editRevision = 0; draftDirty = false; state = MessageSettingsState() }
}
