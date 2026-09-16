package com.novalpie.nativeapp.feature.reader.preferences

import com.novalpie.nativeapp.data.NovalPieApi
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

internal data class ReaderPreferenceProfile(val id: Long, val name: String, val isDefault: Boolean)
internal interface ReaderPreferenceRepository {
    suspend fun list(): List<ReaderPreferenceProfile>
    suspend fun load(name: String): String
    suspend fun create(name: String, preferences: String, isDefault: Boolean)
    suspend fun update(id: Long, preferences: String)
    suspend fun delete(id: Long)
}
internal class WebsiteReaderPreferenceRepository(private val api: NovalPieApi) : ReaderPreferenceRepository {
    private fun checked(raw: Any): Any {
        var value = raw
        repeat(4) {
            val obj = value as? JSONObject ?: return value
            if (obj.has("success") && !obj.optBoolean("success")) throw IOException(obj.optString("message", "服务器拒绝配置操作"))
            value = obj.opt("data")?.takeUnless { it == JSONObject.NULL } ?: return value
        }
        return value
    }
    override suspend fun list(): List<ReaderPreferenceProfile> {
        val array = checked(api.readerPreferenceRequest("GET")) as? JSONArray ?: throw IOException("偏好配置列表格式异常，请重试")
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            ReaderPreferenceProfile(item.getLong("id"), item.getString("config_name"), item.optBoolean("is_default") || item.optInt("is_default") == 1)
        }
    }
    override suspend fun load(name: String): String {
        val raw = checked(api.readerPreferenceRequest("GET", mapOf("config_name" to name))) as? JSONObject ?: throw IOException("配置格式异常")
        return raw.optJSONObject("preferences")?.toString() ?: throw IOException("配置中没有阅读偏好")
    }
    private fun acknowledgement(raw: Any) {
        val value = checked(raw)
        val outer = raw as? JSONObject
        val inner = value as? JSONObject
        if (outer?.optBoolean("success") == true || inner?.optBoolean("success") == true || (inner?.optLong("id") ?: 0) > 0) return
        throw IOException("服务器未明确确认，请刷新配置列表核对；没有自动重发")
    }
    override suspend fun create(name: String, preferences: String, isDefault: Boolean) {
        require(name.isNotBlank()) { "请输入配置名称" }
        val body = JSONObject(preferences).put("config_name", name.trim()).put("is_default", isDefault)
        acknowledgement(api.readerPreferenceRequest("POST", body = body))
    }
    override suspend fun update(id: Long, preferences: String) {
        require(id > 0)
        acknowledgement(api.readerPreferenceRequest("PUT", body = JSONObject(preferences).put("id", id)))
    }
    override suspend fun delete(id: Long) {
        require(id > 0)
        acknowledgement(api.readerPreferenceRequest("DELETE", mapOf("id" to id.toString())))
    }
}
