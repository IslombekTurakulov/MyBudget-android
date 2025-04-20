package ru.iuturakulov.mybudget.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.iuturakulov.mybudget.data.remote.auth.RefreshAuthService
import ru.iuturakulov.mybudget.data.remote.auth.RefreshTokenRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val refreshAuthService: RefreshAuthService,
    private val authEventBus: AuthEventBus
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Не шлём бесконечный цикл на /refresh-token
        if (response.request.url.encodedPath.contains("refresh-token")) {
            return null
        }

        synchronized(this) {
            return try {
                val refreshToken = tokenStorage.getRefreshToken() ?: throw IllegalAccessException()

                val tokensResponse = runBlocking {
                    refreshAuthService
                        .refreshToken(RefreshTokenRequest(refreshToken))
                        .body()!!
                }
                tokenStorage.saveAccessToken(tokensResponse.accessToken)
                tokenStorage.saveRefreshToken(tokensResponse.refreshToken)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokensResponse.accessToken}")
                    .build()
            } catch (e: Exception) {
                tokenStorage.clearTokens()
                CoroutineScope(Dispatchers.Main).launch {
                    authEventBus.publishUnauthorized()
                }
                null
            }
        }
    }
}
