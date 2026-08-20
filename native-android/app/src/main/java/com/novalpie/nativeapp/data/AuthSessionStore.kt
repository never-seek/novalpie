package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.UserProfile
import okio.ByteString.Companion.decodeBase64
import org.json.JSONObject

internal fun decodeAuthTokenProfile(
    token: String,
    nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
): UserProfile? {
    val payloadSegment = token.trim().split('.').getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    val payloadText = payloadSegment.decodeBase64()?.utf8() ?: return null
    val payload = runCatching { JSONObject(payloadText) }.getOrNull() ?: return null
    val expiresAt = payload.valueAsLong("exp")
    if (expiresAt != null && expiresAt <= nowEpochSeconds) return null

    val data = payload.optJSONObject("data") ?: payload
    val id = payload.valueAsLong("sub")
        ?: data.valueAsLong("user_id")
        ?: data.valueAsLong("id")
    val name = data.valueAsString("username")
        ?: data.valueAsString("name")
        ?: payload.valueAsString("username")
    val role = data.valueAsString("role")
        ?: payload.valueAsString("role")
        ?: "user"

    if (id == null && name == null) return null
    return UserProfile(
        id = id,
        name = name ?: "Logged user",
        role = role
    )
}

private fun JSONObject.valueAsString(key: String): String? =
    opt(key)?.toString()?.trim()?.takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.valueAsLong(key: String): Long? =
    opt(key)?.toString()?.trim()?.toLongOrNull()

class AuthSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_auth", Context.MODE_PRIVATE)

    fun loadToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun saveToken(token: String) {
        val normalized = token.trim()
        if (normalized.isBlank()) return
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, normalized)
            .apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
