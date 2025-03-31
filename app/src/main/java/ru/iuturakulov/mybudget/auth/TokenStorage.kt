package ru.iuturakulov.mybudget.auth

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(
    private val encryptedSharedPreferences: SharedPreferences
) {
    private val accessTokenFlow = MutableStateFlow<String?>(null)
    private val refreshTokenFlow = MutableStateFlow<String?>(null)

    init {
        accessTokenFlow.value = encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        refreshTokenFlow.value = encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }

    fun getAccessToken(): String? = accessTokenFlow.value
    fun getRefreshToken(): String? = refreshTokenFlow.value

    fun getAccessTokenFlow(): Flow<String?> = accessTokenFlow.asStateFlow()
    fun getRefreshTokenFlow(): Flow<String?> = refreshTokenFlow.asStateFlow()

    suspend fun saveAccessTokenAsync(newToken: String) {
        withContext(Dispatchers.IO) { saveAccessToken(newToken) }
    }

    fun saveAccessToken(newToken: String) {
        if (accessTokenFlow.value != newToken) {
            accessTokenFlow.value = newToken
            try {
                encryptedSharedPreferences.edit().putString(ACCESS_TOKEN_KEY, newToken).apply()
            } catch (e: Exception) {
                Timber.e("Ошибка сохранения Access Token $e")
            }
        }
    }

    suspend fun saveRefreshTokenAsync(newToken: String) {
        withContext(Dispatchers.IO) { saveRefreshToken(newToken) }
    }

    fun saveRefreshToken(newToken: String) {
        if (refreshTokenFlow.value != newToken) {
            refreshTokenFlow.value = newToken
            try {
                encryptedSharedPreferences.edit().putString(REFRESH_TOKEN_KEY, newToken).apply()
            } catch (e: Exception) {
                Timber.e("Ошибка сохранения Refresh Token $e")
            }
        }
    }

    // Очистка токенов
    fun clearTokens() {
        accessTokenFlow.value = null
        refreshTokenFlow.value = null
        try {
            encryptedSharedPreferences.edit()
                .remove(ACCESS_TOKEN_KEY)
                .remove(REFRESH_TOKEN_KEY)
                .apply()
        } catch (e: Exception) {
            Timber.e("Ошибка очистки токенов $e")
        }
    }

    companion object {
        private const val ACCESS_TOKEN_KEY = "accessToken"
        private const val REFRESH_TOKEN_KEY = "refreshToken"
    }
}
