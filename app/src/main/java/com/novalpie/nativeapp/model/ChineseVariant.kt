package com.novalpie.nativeapp.model

/** Matches the source site's local chinese-variant values. */
enum class ChineseVariant(val persistedValue: String) {
    Original("original"),
    Traditional("traditional"),
    Simplified("simplified");

    companion object {
        fun fromPersisted(value: String?): ChineseVariant =
            entries.firstOrNull { it.persistedValue == value } ?: Original
    }
}

fun ChineseVariant.next(): ChineseVariant = when (this) {
    ChineseVariant.Original -> ChineseVariant.Traditional
    ChineseVariant.Traditional -> ChineseVariant.Simplified
    ChineseVariant.Simplified -> ChineseVariant.Original
}
