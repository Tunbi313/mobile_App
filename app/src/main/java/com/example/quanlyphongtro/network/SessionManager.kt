package com.example.quanlyphongtro.network

import android.content.Context

/**
 * Lưu JWT token và thông tin user vào SharedPreferences
 * để dùng lại sau khi tắt app
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS    = "access_token"
        private const val KEY_REFRESH   = "refresh_token"
        private const val KEY_USER_ID   = "user_id"
        private const val KEY_USERNAME  = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_IS_OWNER  = "is_owner"
    }

    fun saveSession(response: AuthResponse) {
        prefs.edit().apply {
            putString(KEY_ACCESS,    response.access)
            putString(KEY_REFRESH,   response.refresh)
            putInt(KEY_USER_ID,      response.user.id)
            putString(KEY_USERNAME,  response.user.username)
            putString(KEY_FULL_NAME, response.user.full_name)
            putBoolean(KEY_IS_OWNER, response.user.is_owner)
        }.apply()
    }

    fun getAccessToken(): String?  = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun getUsername(): String?     = prefs.getString(KEY_USERNAME, null)
    fun getFullName(): String?    = prefs.getString(KEY_FULL_NAME, null)
    fun getDisplayName(): String  = getFullName() ?: getUsername() ?: "Chủ trọ"
    fun isOwner(): Boolean        = prefs.getBoolean(KEY_IS_OWNER, false)
    fun isLoggedIn(): Boolean     = getAccessToken() != null

    fun bearerToken(): String = "Bearer ${getAccessToken()}"

    fun clearSession() = prefs.edit().clear().apply()
}
