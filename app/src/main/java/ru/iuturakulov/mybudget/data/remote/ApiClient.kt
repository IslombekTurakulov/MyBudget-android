package ru.iuturakulov.mybudget.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.iuturakulov.mybudget.auth.TokenStorage
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://localhost:8080/"

    private fun getAuthInterceptor(@ApplicationContext context: Context): Interceptor {
        return Interceptor { chain ->
            val token = TokenStorage.getToken(context)
            val original: Request = chain.request()
            val requestBuilder = original.newBuilder()
            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            requestBuilder.method(original.method, original.body)
            chain.proceed(requestBuilder.build())
        }
    }

    private fun getLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }

    private fun getHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(getAuthInterceptor(context = context))
            .addInterceptor(getLoggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getRetrofitInstance(@ApplicationContext context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getHttpClient(context = context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}