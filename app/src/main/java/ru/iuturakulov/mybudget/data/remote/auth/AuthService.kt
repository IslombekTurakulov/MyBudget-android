package ru.iuturakulov.mybudget.data.remote.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    // Авторизация пользователя
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenData>

    // Регистрация пользователя
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Void>

    // Восстановление пароля
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Void>

    // Смена пароля
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Void>
}