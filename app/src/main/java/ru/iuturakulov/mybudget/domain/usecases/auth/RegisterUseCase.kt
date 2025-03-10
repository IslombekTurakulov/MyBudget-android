package ru.iuturakulov.mybudget.domain.usecases.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.data.remote.auth.LoginRequest
import ru.iuturakulov.mybudget.data.remote.auth.RegisterRequest
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authService: AuthService,
    private val tokenStorage: TokenStorage
) {

    suspend operator fun invoke(name: String, email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Выполняем регистрацию
                val registerResponse =
                    authService.register(RegisterRequest(name, email, password))
                if (registerResponse.isSuccessful) {
                    // После успешной регистрации пытаемся авторизоваться
                    val loginResponse = authService.login(LoginRequest(email, password))
                    if (loginResponse.isSuccessful) {
                        val body = loginResponse.body()
                        val token = body?.token ?: return@withContext false
                        tokenStorage.saveToken(token) // Сохраняем токен
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                val errorMessage = NetworkErrorHandler.getErrorMessage(e)
                throw Exception(errorMessage)
            }
        }
    }
}