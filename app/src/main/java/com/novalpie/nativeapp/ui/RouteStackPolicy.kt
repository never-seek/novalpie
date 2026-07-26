package com.novalpie.nativeapp.ui

internal fun pushDistinctRoute(stack: List<AppRoute>, route: AppRoute): List<AppRoute> {
    if (stack.lastOrNull() == route) return stack
    return stack + route
}

internal fun replaceTopReaderRoute(stack: List<AppRoute>, route: AppRoute.Reader): List<AppRoute> {
    if (stack.lastOrNull() == route) return stack
    return if (stack.lastOrNull() is AppRoute.Reader) {
        stack.dropLast(1) + route
    } else {
        stack + route
    }
}
