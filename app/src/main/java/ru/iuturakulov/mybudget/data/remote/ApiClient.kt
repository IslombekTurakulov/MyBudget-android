package ru.iuturakulov.mybudget.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.iuturakulov.mybudget.auth.TokenStorage
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://localhost:8080/"

    // Получение Interceptor для авторизации
    private fun getAuthInterceptor(tokenStorage: TokenStorage): Interceptor {
        return Interceptor { chain ->
            val token = tokenStorage.getToken()
            val original: Request = chain.request()
            val requestBuilder = original.newBuilder()
            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            requestBuilder.method(original.method, original.body)
            chain.proceed(requestBuilder.build())
        }
    }

    // Interceptor для логирования
    private fun getLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }

    // Конфигурация OkHttpClient
    private fun getHttpClient(tokenStorage: TokenStorage): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(getAuthInterceptor(tokenStorage))
            .addInterceptor(getLoggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Создание Retrofit instance
    fun getRetrofitInstance(tokenStorage: TokenStorage): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getHttpClient(tokenStorage))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
