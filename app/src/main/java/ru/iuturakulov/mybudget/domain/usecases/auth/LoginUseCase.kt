package ru.iuturakulov.mybudget.domain.usecases.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
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
            try {
                val response = authService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.token ?: return@withContext false
                    tokenStorage.saveToken(token) // Сохраняем токен
                    true
                } else {
                    val errorResponse = response.errorBody()?.string()
                    Timber.e("Ошибка авторизации: $errorResponse")
                    false
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorHandler.getErrorMessage(e)
                Timber.e("Ошибка сети: $errorMessage")
                throw Exception(errorMessage)
            }
        }
    }
}