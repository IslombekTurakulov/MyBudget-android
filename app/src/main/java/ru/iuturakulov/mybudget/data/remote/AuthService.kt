package ru.iuturakulov.mybudget.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    // Авторизация пользователя
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Void>

    // Регистрация пользователя
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Void>

    // Восстановление пароля
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Void>

    // Модели запросов и ответов
    data class LoginRequest(
        val email: String,
        val password: String,
        val userType: String = "admin"
    )

    data class LoginResponse(
        val token: String
    )

    data class RegisterRequest(
        val name: String,
        val email: String,
        val password: String,
        val userType: String = "admin"
    )

    data class ResetPasswordRequest(
        val email: String
    )
}