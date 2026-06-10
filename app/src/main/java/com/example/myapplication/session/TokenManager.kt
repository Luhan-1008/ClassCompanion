package com.example.myapplication.session

import android.content.Context
import android.content.SharedPreferences

// 客户端登录状态保存在 SharedPreferences （本地轻量存储）- 关 App 后还能保留
class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
    }

    // 本地保存token
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // 本地保存userId
    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    // 下次打开 App 还记得你登录过
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
