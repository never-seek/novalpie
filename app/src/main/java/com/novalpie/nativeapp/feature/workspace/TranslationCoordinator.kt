package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class TranslationQueueState(
    val tasks: List<TranslationTask> = emptyList(), val activeId: String? = null,
    val paused: Boolean = false, val error: String? = null,
)

/** Commands and durable phase transitions are serialized; network/model work never holds the lock. */
internal class TranslationCoordinator(
    private val scope: CoroutineScope, private val store: TranslationTaskStore, private val runner: TranslationRunner,
    private val account: () -> Long?, private val config: (Long) -> WorkspaceLocalApiConfig?,
) {
    private val mutable = MutableStateFlow(TranslationQueueState())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private data class Running(val generation: Long, var task: TranslationTask, var job: Job? = null)
    private var active: Running? = null
    @Volatile private var serial = 0L
    private val paused = MutableStateFlow(false)
    private var owner: Long? = null

    suspend fun restore() {
        try { mutex.withLock { restoreLocked() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutex.withLock { mutable.value = mutable.value.copy(error = "任务记录读取失败，请检查可用空间后重试") } }
    }

    private suspend fun restoreLocked() {
        val id = account() ?: return
        if (active != null || owner == id) return
        val tasks = withContext(Dispatchers.IO) { store.load(id) }
        if (account() == id) { owner = id; mutable.value = TranslationQueueState(tasks) }
    }

    private fun command(block: suspend () -> Unit): Job = scope.launch {
        try { mutex.withLock { block() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            mutex.withLock { mutable.value = mutable.value.copy(error = "任务操作未保存，请检查配置、账号及可用空间后重试；未自动重发") }
        }
    }

    fun enqueue(task: TranslationTask): Job = command {
        restoreLocked()
        if (task.accountId != account()) {
            mutable.value = mutable.value.copy(error = "账号已变化，请在当前账号重新确认任务")
            return@command
        }
        if (mutable.value.tasks.any { it.id == task.id || (it.bookId == task.bookId && it.phase !in setOf(TranslationPhase.Completed, TranslationPhase.Cancelled, TranslationPhase.Failed)) }) {
            mutable.value = mutable.value.copy(error = "这本书已有未完成任务，请继续原任务")
            return@command
        }
        val next = if (config(task.configId) == null) task.copy(phase = TranslationPhase.Failed, message = "翻译配置已删除，请重新配置后再试") else task
        persistLocked(next)
        owner = task.accountId
        startNextLocked()
    }

    /** Caller owns mutex. Persist before making a phase observable or allowing a POST. */
    private suspend fun persistLocked(task: TranslationTask) = withContext(NonCancellable) {
        withContext(Dispatchers.IO) { store.save(task) }
        val tasks = mutable.value.tasks
        mutable.value = mutable.value.copy(error = null, tasks = if (tasks.any { it.id == task.id })
            tasks.map { if (it.id == task.id) task else it } else tasks + task)
        active?.takeIf { it.task.id == task.id }?.task = task
    }

    private suspend fun startNextLocked() {
        if (active != null) return
        while (true) {
            val task = mutable.value.tasks.firstOrNull { it.phase == TranslationPhase.Queued && it.accountId == account() } ?: return
            val configuration = config(task.configId)
            if (configuration == null) {
                persistLocked(task.copy(phase = TranslationPhase.Failed, message = "翻译配置已删除，请重新配置"))
                continue
            }
            val running = Running(++serial, task)
            active = running
            paused.value = false
            mutable.value = mutable.value.copy(activeId = task.id, paused = false)
            running.job = scope.launch(start = CoroutineStart.LAZY) { execute(running, configuration) }
            running.job!!.start()
            return
        }
    }

    private suspend fun execute(running: Running, configuration: WorkspaceLocalApiConfig) {
        var interruption: String? = null
        var cancelled = false
        try {
            runner.run(running.task, configuration, {
                currentCoroutineContext().ensureActive()
                paused.first { !it }
                currentCoroutineContext().ensureActive()
                check(account() == running.task.accountId && running.generation == serial) { "账号或网络已变化" }
            }) { progress ->
                mutex.withLock {
                    currentCoroutineContext().ensureActive()
                    check(active === running && running.generation == serial && account() == progress.accountId) { "任务所属会话已失效" }
                    persistLocked(progress)
                }
            }
        } catch (failure: CancellationException) {
            cancelled = true
            interruption = "任务已暂停，可从检查点继续"
            throw failure
        } catch (failure: Exception) {
            interruption = safeTranslationFailure(failure, configuration.apiKey)
        } finally {
            withContext(NonCancellable) { mutex.withLock {
                if (active === running && running.generation == serial) {
                    try {
                        interruption?.let { message -> persistLocked(interrupted(running.task, cancelled, message)) }
                    } catch (_: Exception) {
                        // Never dispatch another task after a durable checkpoint failure.
                        mutable.value = mutable.value.copy(error = "任务检查点保存失败，已停止；重启后按最后检查点核对结果")
                    }
                    active = null
                    mutable.value = mutable.value.copy(activeId = null, paused = false)
                    paused.value = false
                    if (mutable.value.error == null) try { startNextLocked() } catch (_: Exception) {
                        mutable.value = mutable.value.copy(error = "下一任务未能启动，请检查配置或可用空间后重试")
                    }
                }
            } }
        }
    }

    private fun interrupted(task: TranslationTask, cancelled: Boolean, message: String): TranslationTask =
        if (task.phase in setOf(TranslationPhase.Completed, TranslationPhase.Failed, TranslationPhase.Cancelled)) task else task.copy(
        phase = when (task.phase) {
            TranslationPhase.Submitting, TranslationPhase.SubmissionUncertain -> TranslationPhase.SubmissionUncertain
            else -> if (cancelled) TranslationPhase.Paused else TranslationPhase.Failed
        }, message = message,
    )

    fun pause(id: String): Job = command {
        if (active?.task?.id == id) {
            paused.value = true
            mutable.value = mutable.value.copy(paused = true)
        } else mutable.value.tasks.firstOrNull { it.id == id && it.phase == TranslationPhase.Queued }?.let {
            persistLocked(it.copy(phase = TranslationPhase.Paused))
        }
    }

    fun resume(id: String): Job = command { resumeLocked(id) }
    private suspend fun resumeLocked(id: String) {
        if (active?.task?.id == id) { paused.value = false; mutable.value = mutable.value.copy(paused = false); return }
        val task = mutable.value.tasks.firstOrNull { it.id == id } ?: return
        if (task.accountId != account() || task.phase in setOf(TranslationPhase.Completed, TranslationPhase.Cancelled)) return
        if (task.phase == TranslationPhase.SubmissionUncertain) {
            persistLocked(task.copy(message = "提交结果未确认，请先核对网站；未重复发送"))
            return
        }
        persistLocked(task.copy(phase = TranslationPhase.Queued))
        startNextLocked()
    }

    fun cancel(id: String): Job = command {
        if (active?.task?.id == id) { active?.job?.cancel(); paused.value = false; return@command }
        val task = mutable.value.tasks.firstOrNull { it.id == id } ?: return@command
        // Stopping does not turn an unconfirmed server write into a harmless cancelled record.
        if (task.phase == TranslationPhase.SubmissionUncertain) return@command
        persistLocked(task.copy(phase = TranslationPhase.Cancelled))
    }

    fun remove(id: String): Job = command {
        if (active?.task?.id == id || mutable.value.tasks.none { it.id == id && it.accountId == account() }) return@command
        withContext(Dispatchers.IO) { store.remove(id) }
        mutable.value = mutable.value.copy(tasks = mutable.value.tasks.filterNot { it.id == id })
    }

    fun confirmRetryUncertain(id: String): Job = command {
        val task = mutable.value.tasks.firstOrNull { it.id == id && it.phase == TranslationPhase.SubmissionUncertain && it.accountId == account() } ?: return@command
        // Only the separate, explicit duplicate-write warning may clear uncertainty.
        persistLocked(task.copy(phase = TranslationPhase.Queued, message = "已由用户核对并确认重试"))
        startNextLocked()
    }

    fun environmentChanged(): Job = command {
        serial++
        val previous = active
        previous?.job?.cancel()
        try {
            previous?.let { persistLocked(interrupted(it.task, true, "账号或网络已变化，任务暂停；提交结果请核对")) }
        } finally {
            active = null; owner = null; paused.value = false
            mutable.value = TranslationQueueState()
        }
    }
}

internal fun safeTranslationFailure(failure: Exception, apiKey: String): String {
    val message = failure.message.orEmpty()
    val safe = message.takeIf {
        it.length < 240 && (apiKey.isBlank() || !it.contains(apiKey)) && !it.contains("://") &&
            listOf("翻译", "译文", "模型", "插图", "源分块", "准备结果", "没有可翻译", "整章", "标题", "待翻", "账号").any(it::startsWith)
    }
    return safe ?: "翻译任务未完成，请检查网络和本人 API 配置后重试；已完成分块保留"
}
