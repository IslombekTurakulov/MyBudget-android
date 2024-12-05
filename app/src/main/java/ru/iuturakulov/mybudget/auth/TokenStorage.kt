package ru.iuturakulov.mybudget.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(
    private val encryptedSharedPreferences: SharedPreferences
) {

    private var token: String? = null

    // Получение токена из памяти или зашифрованного хранилища
    fun getToken(): String? {
        if (token == null) {
            token = encryptedSharedPreferences.getString("authToken", null)
        }
        return token
    }

    suspend fun saveTokenAsync(newToken: String) {
        withContext(Dispatchers.IO) {
            saveToken(newToken)
        }
    }

    // Сохранение токена в памяти и зашифрованном хранилище
    fun saveToken(newToken: String) {
        try {
            if (token != newToken) {
                token = newToken
                encryptedSharedPreferences
                    .edit()
                    .putString("authToken", newToken)
                    .apply()
            }
        } catch (e: Exception) {
            // Логируем ошибку для отладки
            Timber.e("Ошибка при сохранении токена: ${e.localizedMessage}")
        }
    }


    // Очистка токена из памяти и хранилища
    fun clearToken() {
        token = null
        encryptedSharedPreferences.edit().remove("authToken").apply()
    }
}