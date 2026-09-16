package com.novalpie.nativeapp.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.ui.AppRoute

/** Publish an entire back stack atomically; a reset never exposes an empty/intermediate route. */
internal class AppNavigator(
    initial: List<AppRoute> = listOf(AppRoute.Home),
    private val onNavigationChanged: () -> Unit = {},
) {
    var entries: List<AppRoute> by mutableStateOf(initial.toList().ifEmpty { listOf(AppRoute.Home) })
        private set
    val current: AppRoute get() = entries.last()
    val canGoBack: Boolean get() = entries.size > 1
    var revision: Long = 0L
        private set

    private fun publish(next: List<AppRoute>) {
        revision++
        entries = next
        onNavigationChanged()
    }

    fun push(route: AppRoute) {
        if (current != route) publish(entries + route)
    }

    fun replaceReader(route: AppRoute.Reader) {
        publish(if (current is AppRoute.Reader) entries.dropLast(1) + route else entries + route)
    }

    fun replaceAll(routes: List<AppRoute>) {
        publish(routes.toList().ifEmpty { listOf(AppRoute.Home) })
    }

    fun reset(route: AppRoute) { publish(listOf(route)) }

    fun pop(): Boolean {
        if (!canGoBack) return false
        publish(entries.dropLast(1))
        return true
    }
}
