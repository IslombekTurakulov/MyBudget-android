package ru.iuturakulov.mybudget.domain.usecases.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.data.remote.auth.EmailRequest
import ru.iuturakulov.mybudget.data.remote.auth.LoginRequest
import ru.iuturakulov.mybudget.data.remote.auth.RegisterRequest
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authService: AuthService,
    private val tokenStorage: TokenStorage
) {

    suspend fun sendVerificationCode(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = authService.sendRegisterResetVerifyCode(EmailRequest(email))
                if (response.isSuccessful) {
                    true
                } else {
                    throw Exception("Ошибка отправки кода подтверждения")
                }
            } catch (e: Exception) {
                throw Exception("${e.localizedMessage}")
            }
        }
    }

    suspend fun registerWithVerification(
        name: String,
        email: String,
        password: String,
        verificationCode: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = authService.withVerifyRegistrationWithCode(
                    RegisterRequest(name, email, password, verificationCode)
                )

                if (response.isSuccessful) {
                    // Авторизация после успешной регистрации
                    val loginResponse = authService.login(LoginRequest(email, password))
                    if (loginResponse.isSuccessful) {
                        val body = loginResponse.body() ?: return@withContext false
                        val accessToken = body.accessToken
                        val refreshToken = body.refreshToken
                        tokenStorage.saveAccessTokenAsync(accessToken)
                        tokenStorage.saveRefreshTokenAsync(refreshToken)
                        true
                    } else {
                        throw Exception("Ошибка авторизации после регистрации")
                    }
                } else {
                    throw Exception("${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                throw Exception("${e.localizedMessage}")
            }
        }
    }
}
