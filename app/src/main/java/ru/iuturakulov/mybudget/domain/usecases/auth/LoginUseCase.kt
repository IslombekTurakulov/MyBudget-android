package ru.iuturakulov.mybudget.domain.usecases.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.data.remote.auth.LoginRequest
import timber.log.Timber
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authService: AuthService,
    private val tokenStorage: TokenStorage
) {

    /**
     * Выполняет авторизацию пользователя.
     *
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return true, если авторизация успешна, иначе false
     */
    suspend operator fun invoke(email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val response = authService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext false
                val accessToken = body.accessToken
                val refreshToken = body.refreshToken
                tokenStorage.saveAccessTokenAsync(accessToken)
                tokenStorage.saveRefreshTokenAsync(refreshToken)
                true
            } else {
                val errorResponse = response.errorBody()?.string()
                Timber.e("$errorResponse")
                throw Exception(errorResponse)
            }
        }
    }
}