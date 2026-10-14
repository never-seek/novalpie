package com.novalpie.nativeapp.feature.download

import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

internal enum class DownloadIntegrityCode { MetadataUnavailable, ImageSourceUnavailable, ImageSourceMismatch }
internal data class DownloadIntegrityIssue(val code:DownloadIntegrityCode,val chapter:Int?=null,val sourceHash:String="") {
    val key:String get()="${code.name}:${chapter ?: 0}:$sourceHash"
    val message:String get()=when(code) {
        DownloadIntegrityCode.MetadataUnavailable->"书籍资料/封面来源未确认，可能缺少作者、简介或封面"
        DownloadIntegrityCode.ImageSourceUnavailable->"第${chapter ?: "—"}章正文图源读取失败，保留导出原图引用，未自动去重"
        DownloadIntegrityCode.ImageSourceMismatch->"第${chapter ?: "—"}章导出插图与正文图源不一致，保留原图引用，未自动去重"
    }
}
internal class DownloadIntegrityConfirmationRequired(issues:List<DownloadIntegrityIssue>):IOException(
    "下载完整性尚未确认："+issues.joinToString("；"){it.message}+"。请在App中选择重试或确认保留原文打包。")
internal object DownloadIntegrityJson {
    fun encode(issues:List<DownloadIntegrityIssue>)=JSONArray().apply {issues.forEach {i->put(JSONObject().put("code",i.code.name).put("chapter",i.chapter ?: JSONObject.NULL).put("source",i.sourceHash))}}
    fun decode(value:JSONArray?):List<DownloadIntegrityIssue> = if(value==null)emptyList()else (0 until value.length()).map {index->
        val i=value.getJSONObject(index)
        DownloadIntegrityIssue(DownloadIntegrityCode.valueOf(i.getString("code")),if(i.isNull("chapter"))null else i.getInt("chapter"),i.optString("source")).also {
            require(it.chapter==null||it.chapter>0);require(it.sourceHash.isBlank()||it.sourceHash.matches(Regex("[a-f0-9]{64}")))
        }
    }
}
