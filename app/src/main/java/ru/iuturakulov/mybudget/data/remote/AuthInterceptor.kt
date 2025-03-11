package ru.iuturakulov.mybudget.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.data.remote.auth.RefreshTokenRequest
import timber.log.Timber

class AuthInterceptor(
    private val tokenStorage: TokenStorage,
    private val apiService: AuthService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = tokenStorage.getAccessToken()

        val originalRequest = chain.request().newBuilder()
        if (!accessToken.isNullOrEmpty()) {
            originalRequest.header("Authorization", "Bearer $accessToken")
        }

        val response = chain.proceed(originalRequest.build())

        // Если получили 401 - пробуем обновить токен
        if (response.code == 401) {
            synchronized(this) {
                val updatedAccessToken = runBlocking { refreshToken() }
                if (!updatedAccessToken.isNullOrEmpty()) {
                    tokenStorage.saveAccessToken(updatedAccessToken)

                    // Повторяем запрос с новым токеном
                    val newRequest = originalRequest
                        .header("Authorization", "Bearer $updatedAccessToken")
                        .build()
                    response.close() // Закрываем старый response перед повторным вызовом
                    return chain.proceed(newRequest)
                } else {
                    tokenStorage.clearTokens() // Очистка токенов при ошибке refresh
                }
            }
        }

        return response
    }

    private suspend fun refreshToken(): String? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null

        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.refreshToken(RefreshTokenRequest(refreshToken))
            }

            if (response.isSuccessful) {
                val token = response.body()
                token?.also { newAccessToken ->
                    tokenStorage.saveAccessTokenAsync(newAccessToken)
                    tokenStorage.saveRefreshTokenAsync(token)
                }
            } else {
                tokenStorage.clearTokens()
                null
            }
        } catch (e: Exception) {
           Timber.e("Ошибка обновления токена: ${e.message}")
            null
        }
    }
}
