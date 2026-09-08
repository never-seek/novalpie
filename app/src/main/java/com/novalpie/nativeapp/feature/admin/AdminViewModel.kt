package com.novalpie.nativeapp.feature.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import java.util.EnumMap

/** Section reads are independent of the single confirmed mutation; refresh never releases it. */
internal class AdminViewModel(private val repository: AdminRepository, private val allowed: () -> Boolean, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private val reads = EnumMap<AdminSection, Job>(AdminSection::class.java)
    private val serials = EnumMap<AdminSection, Long>(AdminSection::class.java)
    private data class Notice(val text: String?, val error: Boolean = false, val uncertain: Boolean = false, val draft: AdminEditDraft? = null)
    private val notices = EnumMap<AdminSection, Notice>(AdminSection::class.java)
    private var active: AdminCommand? = null
    private var writeJob: Job? = null
    private var environment = 0L
    var state by mutableStateOf(AdminState())
        private set
    fun present(change: (AdminState) -> AdminState) { if (allowed()) state = change(state).copy(actionLoading = active != null) }
    fun enter(section: AdminSection) { if (!allowed()) { environmentChanged(); return }; state = withNotice(state.copy(section = section)) }
    private fun withNotice(value: AdminState): AdminState {
        val note = notices[value.section]
        return value.copy(actionLoading = active != null, actionMessage = note?.text, actionError = note?.error == true,
            actionUncertain = note?.uncertain == true, failedEditDraft = note?.draft)
    }
    private fun notify(section: AdminSection, note: Notice) { notices[section] = note; state = withNotice(state) }
    fun load(section: AdminSection = state.section) { enter(section); if (allowed()) refresh(section) }
    private fun refresh(section: AdminSection) {
        if (!allowed()) return
        val serial = (serials[section] ?: 0) + 1; serials[section] = serial
        val account = environment; val query = state
        reads.remove(section)?.cancel()
        state = when (section) {
            AdminSection.Overview -> state.copy(overview = LoadResult.Loading)
            AdminSection.Review -> state.copy(reviewSettings = LoadResult.Loading, reviewRequests = LoadResult.Loading)
            AdminSection.Keys -> state.copy(keys = LoadResult.Loading, baseUrlRules = LoadResult.Loading)
            AdminSection.OperationLogs -> state.copy(operationLogs = LoadResult.Loading)
            AdminSection.Scraper -> state.copy(cookieConfigs = LoadResult.Loading, schedulerLogs = LoadResult.Loading)
            AdminSection.Shop -> state.copy(shopItems = LoadResult.Loading)
        }
        reads[section] = work.launch {
            suspend fun <T> read(label: String, request: suspend () -> T, apply: (AdminState, LoadResult<T>) -> AdminState) {
                val result = try { LoadResult.Success(request()) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { LoadResult.Error(apiFailureMessage(label, failure)) }
                if (account == environment && serials[section] == serial && allowed()) state = apply(state, result)
            }
            coroutineScope {
                when (section) {
                    AdminSection.Overview -> read("管理总览", { repository.overview(query.overviewDays) }) { s, r -> s.copy(overview = r) }
                    AdminSection.Review -> {
                        launch { read("审核设置", repository::reviewSettings) { s, r -> s.copy(reviewSettings = r) } }
                        launch { read("审核请求", { repository.reviews(query.reviewQuery) }) { s, r -> s.copy(reviewRequests = r) } }
                    }
                    AdminSection.Keys -> {
                        launch { read("Key 管理", repository::keys) { s, r -> s.copy(keys = r) } }
                        launch { read("BaseURL 规则", repository::rules) { s, r -> s.copy(baseUrlRules = r) } }
                    }
                    AdminSection.OperationLogs -> read("操作日志", { repository.logs(query.operationLogQuery) }) { s, r -> s.copy(operationLogs = r) }
                    AdminSection.Scraper -> {
                        launch { read("Cookie 配置", repository::cookies) { s, r -> s.copy(cookieConfigs = r) } }
                        launch { read("调度日志", { repository.scheduler(query.schedulerLines) }) { s, r -> s.copy(schedulerLogs = r) } }
                    }
                    AdminSection.Shop -> read("商店商品", { repository.shop(query.shopQuery) }) { s, r -> s.copy(shopItems = r) }
                }
            }
        }
    }
    fun mutate(command: AdminCommand) {
        if (!allowed()) { environmentChanged(); return }
        if (active != null) return
        active = command
        val account = environment
        notify(command.section, Notice("${command.label}…"))
        writeJob = work.launch {
            val result = try { Result.success(repository.mutate(command)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { Result.failure(failure) }
            if (account != environment || !allowed() || active !== command) return@launch
            active = null
            val response = result.getOrNull()
            if (response?.success == true) {
                notify(command.section, Notice(response.message ?: command.successMessage))
                refresh(command.section) // Update that section's data without navigating back to it.
            } else {
                val failure = result.exceptionOrNull()
                val uncertain = failure != null && failure !is com.novalpie.nativeapp.data.AdminActionRejectedException
                var message = response?.message ?: failure?.let { apiFailureMessage(command.label, it) } ?: "操作被服务器拒绝"
                (command as? AdminCommand.SaveCookie)?.raw?.takeIf { it.isNotBlank() }?.let { message = message.replace(it, "[已隐藏]") }
                if (uncertain) message += "；结果未确认，请先核对站点，未自动重发"
                notify(command.section, Notice(message, error = true, uncertain = uncertain, draft = command.editDraft()))
            }
        }
    }
    fun environmentChanged() {
        environment++; reads.values.forEach { it.cancel() }; reads.clear(); serials.clear()
        writeJob?.cancel(); writeJob = null; active = null; notices.clear(); state = AdminState(accessRevision = environment)
    }
    fun close() { work.cancel() }
    override fun onCleared() { close() }
}
