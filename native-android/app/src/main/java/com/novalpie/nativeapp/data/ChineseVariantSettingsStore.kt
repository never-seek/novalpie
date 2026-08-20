package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.ChineseVariant

/** Local-only counterpart of the source chinese-variant / opencc-enabled browser preferences. */
class ChineseVariantSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadVariant(): ChineseVariant =
        ChineseVariant.fromPersisted(prefs.getString(KEY_VARIANT, null))

    fun saveVariant(variant: ChineseVariant) {
        prefs.edit().putString(KEY_VARIANT, variant.persistedValue).apply()
    }

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_chinese_variant"
        private const val KEY_VARIANT = "chinese_variant"
    }
}
