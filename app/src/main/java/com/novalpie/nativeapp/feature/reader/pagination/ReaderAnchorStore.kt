package com.novalpie.nativeapp.feature.reader.pagination

import android.content.Context
import org.json.JSONObject

/** Separate additive schema; reading/saving a Beta7 anchor never clears Beta6 paragraph progress. */
internal class ReaderAnchorStore(context:Context) {
    private val preferences=context.getSharedPreferences("novalpie_native_reader_anchors",Context.MODE_PRIVATE)
    fun save(anchor:ReaderAnchor) {
        if(anchor.bookId<=0||anchor.chapterId<=0||anchor.blockId.isBlank())return
        preferences.edit().putString(key(anchor.bookId,anchor.chapterId),JSONObject()
            .put("version",1).put("block",anchor.blockId).put("offset",anchor.textOffset.coerceAtLeast(0)).toString()).apply()
    }
    fun load(bookId:Long,chapterId:Long):ReaderAnchor? {
        val raw=preferences.getString(key(bookId,chapterId),null) ?: return null
        return runCatching {
            val json=JSONObject(raw)
            if(json.getInt("version")!=1)return null
            val block=json.getString("block").takeIf(String::isNotBlank) ?: return null
            ReaderAnchor(bookId,chapterId,block,json.getInt("offset").coerceAtLeast(0))
        }.getOrNull()
    }
    private fun key(bookId:Long,chapterId:Long)="book_${bookId}_chapter_$chapterId"
}
