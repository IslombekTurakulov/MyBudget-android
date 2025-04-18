package ru.iuturakulov.mybudget.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.auth.RefreshAuthService
import ru.iuturakulov.mybudget.data.remote.auth.RefreshTokenRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val refreshAuthService: RefreshAuthService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Проверяем, не является ли запрос запросом на обновление токена
        if (response.request.url.encodedPath.contains("refresh-token")) {
            return null
        }

        synchronized(this) {
            val refreshToken = tokenStorage.getRefreshToken() ?: return null

            return try {
                val newToken = runBlocking {
                    refreshAuthService.refreshToken(RefreshTokenRequest(refreshToken))
                }.body() ?: return null

                tokenStorage.saveAccessToken(newToken)

                response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            } catch (e: Exception) {
                Timber.e("Failed to refresh token: ${e.message}")
                tokenStorage.clearTokens()
                null
            }
        }
    }
}