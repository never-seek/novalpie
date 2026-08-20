package com.novalpie.nativeapp.model

/**
 * Local persistence policy used by the source favourites page.
 *
 * This controls only presentation state. It never changes remote favourites, groups, or history.
 */
enum class FavoritesCacheMode(val persistedValue: String) {
    None("none"),
    NoSearch("no-search"),
    All("all");

    fun next(): FavoritesCacheMode = when (this) {
        None -> NoSearch
        NoSearch -> All
        All -> None
    }

    companion object {
        fun fromPersisted(value: String?): FavoritesCacheMode =
            entries.firstOrNull { it.persistedValue == value } ?: All
    }
}
