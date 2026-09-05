package com.novalpie.nativeapp.feature.reader.pagination

internal enum class ChapterDirection { Next, Previous }

internal sealed interface PageMove {
    data class Page(val index: Int) : PageMove
    data class Chapter(val requestId: Long, val direction: ChapterDirection) : PageMove
    data object Busy : PageMove
}

/** A single event machine for touch, volume and toolbar paging. Owned by the reader ViewModel. */
internal class ReaderPageNavigator(initial: PagePlan, anchor: ReaderAnchor? = null) {
    var plan = initial
        private set
    var pageIndex = anchor?.let(initial::pageForAnchor) ?: 0
        private set
    private var requestSerial = 0L
    private var pending: PageMove.Chapter? = null

    fun next(): PageMove = move(ChapterDirection.Next)
    fun previous(): PageMove = move(ChapterDirection.Previous)

    private fun move(direction: ChapterDirection): PageMove {
        if (pending != null) return PageMove.Busy
        val target = pageIndex + if (direction == ChapterDirection.Next) 1 else -1
        if (target in plan.pages.indices) {
            pageIndex = target
            return PageMove.Page(target)
        }
        return PageMove.Chapter(++requestSerial, direction).also { pending = it }
    }

    fun acceptChapter(requestId: Long, next: PagePlan): Boolean {
        val request = pending?.takeIf { it.requestId == requestId } ?: return false
        if (next.key.bookId != plan.key.bookId || next.key.chapterId == plan.key.chapterId) return false
        plan = next
        pageIndex = if (request.direction == ChapterDirection.Previous) next.pages.lastIndex else 0
        pending = null
        return true
    }

    fun failChapter(requestId: Long) {
        if (pending?.requestId == requestId) pending = null
    }

    fun replaceLayout(next: PagePlan, anchor: ReaderAnchor = plan.pages[pageIndex].startAnchor): Boolean {
        if (next.key.bookId != plan.key.bookId || next.key.chapterId != plan.key.chapterId) return false
        plan = next
        pageIndex = next.pageForAnchor(anchor)
        return true
    }

    fun jump(next: PagePlan, anchor: ReaderAnchor? = null) {
        pending = null
        requestSerial++
        plan = next
        pageIndex = anchor?.let(next::pageForAnchor) ?: 0
    }
}

/** Invalidating a layout is cheap; jobs may finish off-thread but only the current ticket publishes. */
internal class PageLayoutGeneration {
    data class Ticket(val generation: Long, val key: PageLayoutKey)
    private var serial = 0L
    private var current: Ticket? = null

    @Synchronized fun request(key: PageLayoutKey): Ticket = Ticket(++serial, key).also { current = it }
    @Synchronized fun invalidate() { serial++; current = null }
    @Synchronized fun isCurrent(ticket: Ticket): Boolean = current == ticket
}
