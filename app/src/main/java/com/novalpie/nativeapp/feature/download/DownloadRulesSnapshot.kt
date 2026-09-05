package com.novalpie.nativeapp.feature.download

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.ReaderDownloadReplacementSnapshot
import com.novalpie.nativeapp.ui.ReaderReplacementState
import com.novalpie.nativeapp.ui.readerDownloadReplacementSnapshot
import org.json.JSONArray
import org.json.JSONObject

/** Immutable, private task serialization. Does not publish rules or change the user's preferences. */
internal object DownloadRulesSnapshot {
    fun encode(state:ReaderReplacementState):String=JSONObject()
        .put("version",1).put("book",state.novelId).put("shared_enabled",state.sharedRulesEnabled)
        .put("hidden",JSONArray(state.hiddenSharedRuleIds.toList()))
        .put("personal",rules(state.personalRules)).put("shared",rules(state.availableSharedRules)).toString()

    fun decode(raw:String):ReaderDownloadReplacementSnapshot {
        val json=JSONObject(raw)
        require(json.getInt("version")==1)
        val bookId=json.getLong("book")
        return readerDownloadReplacementSnapshot(true,ReaderReplacementState(
            novelId=bookId,personalRules=readRules(json.getJSONArray("personal"),bookId,ReaderReplacementOwner.Personal),
            sharedRules=LoadResult.Success(readRules(json.getJSONArray("shared"),bookId,ReaderReplacementOwner.Shared)),
            hiddenSharedRuleIds=json.getJSONArray("hidden").let{a->(0 until a.length()).map{a.getString(it)}.toSet()},
            sharedRulesEnabledOverride=json.getBoolean("shared_enabled"),
        ))
    }
    private fun rules(values:List<ReaderReplacementRule>)=JSONArray().apply {
        values.forEach {r->put(JSONObject().put("id",r.id).put("source",r.source).put("replacement",r.replacement)
            .put("website",r.websiteRuleId ?: JSONObject.NULL).put("shared_id",r.sharedRuleId ?: JSONObject.NULL)
            .put("enabled",r.isEnabled).put("regex",r.isRegex).put("flags",JSONArray(r.regexFlags.map{it.name}))
            .put("order",r.order).put("target",r.target.name)
            .put("scope",when(val s=r.scope){ReaderReplacementScope.WholeBook->"all";is ReaderReplacementScope.CurrentChapter->"${s.chapterOrder}:${s.chapterOrder}";is ReaderReplacementScope.ChapterRange->"${s.startOrder}:${s.endOrder}"})) }
    }
    private fun readRules(array:JSONArray,bookId:Long,owner:ReaderReplacementOwner)= (0 until array.length()).map {index->
        val r=array.getJSONObject(index)
        val scope=r.getString("scope")
        ReaderReplacementRule(id=r.getString("id"),novelId=bookId,source=r.getString("source"),replacement=r.getString("replacement"),owner=owner,
            websiteRuleId=if(r.isNull("website"))null else r.getLong("website"),sharedRuleId=if(r.isNull("shared_id"))null else r.getString("shared_id"),
            isEnabled=r.getBoolean("enabled"),isRegex=r.getBoolean("regex"),order=r.getInt("order"),target=ReaderReplacementTarget.valueOf(r.getString("target")),
            regexFlags=r.getJSONArray("flags").let{a->(0 until a.length()).map{ReaderReplacementRegexFlag.valueOf(a.getString(it))}.toSet()},
            scope=if(scope=="all")ReaderReplacementScope.WholeBook else scope.split(':').let{ReaderReplacementScope.ChapterRange(it[0].toInt(),it[1].toInt())})
    }
}
