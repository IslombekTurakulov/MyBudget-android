package ru.iuturakulov.mybudget.auth

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(
    private val encryptedSharedPreferences: SharedPreferences
) {
    @Volatile
    private var token: String? = null

    fun getToken(): String? {
        return token ?: synchronized(this) {
            token ?: encryptedSharedPreferences.getString("authToken", null).also {
                token = it
            }
        }
    }

    suspend fun saveTokenAsync(newToken: String) {
        withContext(Dispatchers.IO) {
            saveToken(newToken)
        }
    }

    fun saveToken(newToken: String) {
        synchronized(this) {
            if (token != newToken) {
                token = newToken
                encryptedSharedPreferences
                    .edit()
                    .putString("authToken", newToken)
                    .apply()
            }
        }
    }

    fun clearToken() {
        synchronized(this) {
            token = null
            encryptedSharedPreferences.edit().remove("authToken").apply()
        }
    }
}
