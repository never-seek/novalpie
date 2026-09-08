package com.novalpie.nativeapp.feature.forum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID

/** Per-post state. Reads can refresh independently while a write retains its immutable identity. */
internal class ForumPostViewModel(private val repository: ForumPostRepository, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private class Entry(val id: Long) {
        var state = ForumPostDetailState(postId = id)
        var bodySerial = 0L; var commentsSerial = 0L; var referencesSerial = 0L
        var bodyJob: Job? = null; var commentsJob: Job? = null; var referencesJob: Job? = null
        var writing = false
        var scroll = GridScrollPosition()
        val pendingEchoes = linkedMapOf<Pair<Long?, Long>, ForumComment>()
    }
    private val cache = LinkedHashMap<Long, Entry>(8, .75f, true)
    private var current: Entry? = null
    private var environment = 0L
    var state by mutableStateOf(ForumPostDetailState())
        private set
    private fun update(entry: Entry, transform: (ForumPostDetailState) -> ForumPostDetailState) {
        entry.state = transform(entry.state)
        if (current === entry) state = entry.state
    }
    fun present(transform: (ForumPostDetailState) -> ForumPostDetailState) { current?.let { update(it, transform) } }
    fun enter(id: Long) {
        cache[id]?.let { current = it; state = it.state } ?: load(id)
    }
    fun scroll(id: Long): GridScrollPosition = cache[id]?.scroll ?: GridScrollPosition()
    fun saveScroll(id: Long, index: Int, offset: Int) { cache[id]?.scroll = GridScrollPosition.from(index, offset) }
    fun load(id: Long, message: String? = null, retainComments: Boolean = false) {
        if (id <= 0) return
        val entry = cache.getOrPut(id) { Entry(id) }
        current = entry; state = entry.state
        update(entry) { it.copy(actionMessage = message ?: it.actionMessage) }
        while (cache.size > 6) {
            val victim = cache.values.firstOrNull { it !== current && !it.writing && it.state.commentDraft.isEmpty() } ?: break
            victim.bodyJob?.cancel(); victim.commentsJob?.cancel(); victim.referencesJob?.cancel(); cache.remove(victim.id)
        }
        readBody(entry); readComments(entry, retainComments)
    }
    fun retryBody() { current?.let(::readBody) }
    fun retryComments() { current?.let { readComments(it, true) } }
    fun loadMore() { current?.let { if (it.state.commentsHasMore && !it.state.commentsLoadingMore) readComments(it, true, append = true) } }
    private fun readBody(entry: Entry) {
        val serial = ++entry.bodySerial; val account = environment
        entry.bodyJob?.cancel()
        if (entry.state.detail !is LoadResult.Success) update(entry) { it.copy(detail = LoadResult.Loading) }
        entry.bodyJob = work.launch {
            val result = attempt { repository.detail(entry.id).also { check(it.post.id == entry.id) { "帖子身份不符" } } }
            if (account != environment || serial != entry.bodySerial) return@launch
            update(entry) { forumPostDetailWithLoadedDetail(it, result) }
            readReferences(entry)
        }
    }
    private fun readComments(entry: Entry, retain: Boolean, append: Boolean = false) {
        val serial = ++entry.commentsSerial; val account = environment
        entry.commentsJob?.cancel()
        if (!retain || entry.state.comments !is LoadResult.Success) update(entry) { it.copy(comments = LoadResult.Loading) }
        val target = if (append) entry.state.commentsPage + 1 else entry.state.commentsPage
        update(entry) { it.copy(commentsLoadingMore = append) }
        entry.commentsJob = work.launch {
            val result = attempt {
                val loaded = (if (append) target..target else 1..target).map { page -> repository.commentPage(entry.id, page).also { result ->
                    check(result.page == page && result.items.none { it.postId != null && it.postId != entry.id }) { "评论身份或页码不符" }
                } }
                ForumCommentsPage(loaded.flatMap { it.items }, target, loaded.last().hasMore)
            }
            if (account != environment || serial != entry.commentsSerial) return@launch
            result.getOrNull()?.items?.forEach { entry.pendingEchoes.remove(it.parentCommentId to it.id) }
            update(entry) { state ->
                val next = if (result.isSuccess) {
                    val pageItems = result.getOrThrow().items
                    val items = if (append) ((state.comments as? LoadResult.Success)?.value.orEmpty() + pageItems).distinctBy { it.parentCommentId to it.id }
                        else pageItems
                    state.copy(comments = LoadResult.Success((items + entry.pendingEchoes.values).distinctBy { it.parentCommentId to it.id }))
                } else forumPostDetailWithLoadedComments(state, result.map { it.items }, retain)
                next.copy(commentsPage = if (result.isSuccess) target else state.commentsPage,
                    commentsHasMore = result.getOrNull()?.hasMore ?: state.commentsHasMore, commentsLoadingMore = false,
                    actionMessage = if (append && result.isFailure) "更多评论加载失败，可重试" else next.actionMessage)
            }
            readReferences(entry)
        }
    }
    private fun readReferences(entry: Entry) {
        val texts = listOfNotNull((entry.state.detail as? LoadResult.Success)?.value?.content) +
            (entry.state.comments as? LoadResult.Success)?.value.orEmpty().map { it.content }
        val ids = forumBookReferenceIds(texts)
        val account = environment; val serial = ++entry.referencesSerial
        entry.referencesJob?.cancel()
        val previous = entry.state.bookReferences
        update(entry) { it.copy(bookReferences = ids.associateWith { id -> previous[id] ?: LoadResult.Loading }) }
        entry.referencesJob = work.launch {
            val gate = Semaphore(4)
            coroutineScope { ids.filter { previous[it] !is LoadResult.Success }.map { id -> launch {
                val result = gate.withPermit { attempt { repository.book(id) } }
                if (account == environment && serial == entry.referencesSerial) update(entry) { state ->
                    state.copy(bookReferences = state.bookReferences + (id to result.fold({ LoadResult.Success(it) }, { LoadResult.Error("关联书籍加载失败，可刷新重试") })))
                }
            } }.joinAll() }
        }
    }
    fun draft(value: String) {
        val entry = current ?: return
        update(entry) { it.copy(commentDraft = value, commentClientRequestId = null) }
    }
    fun reply(comment: ForumComment) {
        val entry = current ?: return
        val root = forumReplySubmissionCommentId(comment, (entry.state.comments as? LoadResult.Success)?.value.orEmpty())
        update(entry) { it.copy(replyingToCommentId = root, replyingToName = comment.authorName, commentClientRequestId = null,
            commentDraft = replyComposerDraftForTarget(it.commentDraft, it.replyingToName, comment.authorName)) }
    }
    fun cancelReply() { present { it.copy(replyingToCommentId = null, replyingToName = null, commentClientRequestId = null) } }
    fun toggleReplies(id: Long) { present { it.copy(expandedCommentIds = if (id in it.expandedCommentIds) it.expandedCommentIds - id else it.expandedCommentIds + id) } }
    fun send() {
        val entry = current ?: return
        if (entry.writing || entry.state.commentDraft.isBlank()) return
        val before = entry.state
        val payload = ForumCommentDraft(before.commentDraft.trim(), before.replyingToCommentId, before.replyingToName,
            before.commentClientRequestId ?: UUID.randomUUID().toString())
        val account = environment
        entry.writing = true
        update(entry) { it.copy(actionLoading = true, actionMessage = null, commentClientRequestId = payload.requestId) }
        work.launch {
            val result = attempt { repository.send(entry.id, payload).also {
                check(it.reply?.postId == null || it.reply.postId == entry.id) { "回复回执的帖子身份不符，请刷新核对后重试" }
            } }
            if (account != environment) return@launch
            entry.writing = false
            result.getOrNull()?.takeIf { it.success }?.reply?.let { echo -> entry.pendingEchoes[echo.parentCommentId to echo.id] = echo }
            update(entry) { active ->
                val sameDraft = active.commentClientRequestId == payload.requestId && active.commentDraft.trim() == payload.content &&
                    active.replyingToCommentId == payload.parentId && active.replyingToName == payload.replyName
                val handled = forumPostDetailAfterCommentSubmission(active, result).copy(actionLoading = false)
                if (sameDraft) handled else handled.copy(commentDraft = active.commentDraft, replyingToCommentId = active.replyingToCommentId,
                    replyingToName = active.replyingToName, commentClientRequestId = active.commentClientRequestId)
            }
            if (result.getOrNull()?.success == true) { readComments(entry, true); readBody(entry) }
        }
    }
    fun toggleVote(optionId: Long) {
        val poll = (state.detail as? LoadResult.Success)?.value?.poll ?: return
        if (poll.isClosed || poll.userVoteOptionIds.isNotEmpty()) return
        present { it.copy(selectedPollOptionIds = forumPollSelectedOptionIdsAfterToggle(it.selectedPollOptionIds, optionId, poll.allowMultiple, poll.maxChoices)) }
    }
    fun vote(authenticated: Boolean) {
        val entry = current ?: return
        val poll = (entry.state.detail as? LoadResult.Success)?.value?.poll ?: return
        val options = entry.state.selectedPollOptionIds.toList()
        if (!forumPollCanSubmit(authenticated, poll.isClosed, poll.userVoteOptionIds, options.toSet())) return
        mutate(entry, "投票") { repository.vote(entry.id, options) }
    }
    fun react(reaction: ForumReaction, comment: ForumComment? = null) {
        val entry = current ?: return
        val target = comment?.let { forumCommentActionTarget(it, (entry.state.comments as? LoadResult.Success)?.value.orEmpty()) }
        mutate(entry, reaction.label) { repository.react(entry.id, reaction, target?.parentCommentId, target?.replyId) }
    }
    private fun mutate(entry: Entry, label: String, operation: suspend () -> ForumActionResult) {
        if (entry.writing) return
        val account = environment
        entry.writing = true
        update(entry) { it.copy(actionLoading = true, actionMessage = null) }
        work.launch {
            val result = attempt(operation)
            if (account != environment) return@launch
            entry.writing = false
            update(entry) { it.copy(actionLoading = false, actionMessage = result.fold(
                { answer -> answer.message ?: if (answer.success) "$label 已同步" else "$label 被拒绝，请重试" },
                { error -> apiFailureMessage(label, error) })) }
            if (result.getOrNull()?.success == true) { readBody(entry); readComments(entry, true) }
        }
    }
    fun environmentChanged() { environment++; work.coroutineContext.cancelChildren(); cache.clear(); current = null; state = ForumPostDetailState() }
    fun close() { work.cancel() }
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try { Result.success(block()) }
        catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }
}
