package ru.iuturakulov.mybudget.data.remote.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    // Авторизация пользователя
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenData>

    @POST("auth/verify-email-registration")
    suspend fun withVerifyRegistrationWithCode(@Body request: RegisterRequest): Response<Any>

    @POST("auth/verify-reset-code")
    suspend fun withVerifyResetPasswordWithCode(@Body request: VerifyEmailRequest): Response<Any>

    @POST("auth/request-reset-password-code")
    suspend fun sendPasswordResetVerifyCode(@Body request: EmailRequest): Response<String>

    @POST("auth/request-register-code")
    suspend fun sendRegisterResetVerifyCode(@Body request: EmailRequest): Response<Unit>
}

interface ChangePasswordAuthService {
    // Смена пароля
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Void>
}

interface RefreshAuthService {

    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokensResponse>
}