package com.novalpie.nativeapp.model

/** Native equivalent of the source site's local `theme` state, with an Android system default. */
enum class AppThemeMode(val persistedValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromPersisted(value: String?): AppThemeMode =
            entries.firstOrNull { it.persistedValue == value } ?: System
    }
}
