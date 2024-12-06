package ru.iuturakulov.mybudget.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
import ru.iuturakulov.mybudget.data.remote.AuthService
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
                    authService.register(AuthService.RegisterRequest(name, email, password))
                if (registerResponse.isSuccessful) {
                    // После успешной регистрации пытаемся авторизоваться
                    val loginResponse = authService.login(AuthService.LoginRequest(email, password))
                    if (loginResponse.isSuccessful) {
                        val token = loginResponse.body()?.token ?: return@withContext false
                        tokenStorage.saveToken(token)
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