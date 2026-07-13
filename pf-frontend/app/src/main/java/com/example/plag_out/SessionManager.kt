package com.example.plag_out

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val JWT_TOKEN = "jwt_token"
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(JWT_TOKEN, token).apply()
    }

    fun fetchAccessToken(): String? {
        return prefs.getString(JWT_TOKEN, null)
    }

    fun clearAccessToken() {
        prefs.edit().remove(JWT_TOKEN).apply()
    }
}
