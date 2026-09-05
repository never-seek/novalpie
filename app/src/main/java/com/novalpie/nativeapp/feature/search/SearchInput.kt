package com.novalpie.nativeapp.feature.search

import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.ui.convertChineseVariantText

internal data class SearchWordRangeInput(val value:String?=null,val error:String?=null)
internal fun searchWordRangeInput(minimum:String,maximum:String):SearchWordRangeInput {
    val min=minimum.trim().takeIf(String::isNotEmpty)?.toLongOrNull()
    val max=maximum.trim().takeIf(String::isNotEmpty)?.toLongOrNull()
    if((minimum.isNotBlank()&&min==null)||(maximum.isNotBlank()&&max==null)||(min!=null&&min<0)||(max!=null&&max<0))return SearchWordRangeInput(error="请输入有效的非负字数")
    if(min!=null&&max!=null&&min>max)return SearchWordRangeInput(error="最小字数不能大于最大字数")
    return SearchWordRangeInput(value=if(min==null&&max==null)"" else "${min ?: ""}..${max ?: ""}")
}
internal fun searchTagQueryMatches(tag:String,query:String):Boolean {
    if(query.isBlank())return true
    return convertChineseVariantText(tag,ChineseVariant.Simplified).contains(convertChineseVariantText(query.trim(),ChineseVariant.Simplified),ignoreCase=true)
}
internal val searchWordRangeStops=listOf(0L,50_000L,300_000L,500_000L,1_000_000L,2_000_000L,5_000_000L,10_000_000L,null)
