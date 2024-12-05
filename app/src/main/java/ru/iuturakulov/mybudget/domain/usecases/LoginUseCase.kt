package ru.iuturakulov.mybudget.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.AuthService
import ru.iuturakulov.mybudget.data.remote.LoginRequest
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
                    val token = response.body()?.token ?: return@withContext false
                    tokenStorage.saveToken(token)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                throw Exception("Ошибка подключения: ${e.message}")
            }
        }
    }
}