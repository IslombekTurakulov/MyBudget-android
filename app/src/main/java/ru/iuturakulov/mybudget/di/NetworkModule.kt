package ru.iuturakulov.mybudget.di

import android.content.SharedPreferences
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.iuturakulov.mybudget.auth.AuthEventBus
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.AnalyticsService
import ru.iuturakulov.mybudget.data.remote.AuthInterceptor
import ru.iuturakulov.mybudget.data.remote.NotificationsService
import ru.iuturakulov.mybudget.data.remote.ParticipantsService
import ru.iuturakulov.mybudget.data.remote.ProjectService
import ru.iuturakulov.mybudget.data.remote.SettingsService
import ru.iuturakulov.mybudget.auth.TokenAuthenticator
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.data.remote.auth.ChangePasswordAuthService
import ru.iuturakulov.mybudget.data.remote.auth.RefreshAuthService
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenStorage(sharedPreferences: SharedPreferences): TokenStorage {
        return TokenStorage(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideAuthEventBus(): AuthEventBus {
        return AuthEventBus()
    }

    @Provides
    @Singleton
    fun provideBaseOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(getDefaultHeadersInterceptor())
            .addInterceptor(getLoggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(baseOkHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Сервис для обновления токенов (использует базовый клиент)
    @Provides
    @Singleton
    fun provideRefreshAuthService(@Named("AuthRetrofit") retrofit: Retrofit): RefreshAuthService {
        return retrofit.create(RefreshAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStorage: TokenStorage): AuthInterceptor {
        return AuthInterceptor(tokenStorage)
    }

    @Provides
    @Singleton
    @Named("AuthenticatedOkHttp")
    fun provideAuthenticatedOkHttpClient(
        baseOkHttpClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return baseOkHttpClient.newBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthenticatedRetrofit")
    fun provideAuthenticatedRetrofit(
        @Named("AuthenticatedOkHttp") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(@Named("AuthRetrofit") retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideProjectService(@Named("AuthenticatedRetrofit") retrofit: Retrofit): ProjectService {
        return retrofit.create(ProjectService::class.java)
    }

    @Provides
    @Singleton
    fun provideSettingsService(@Named("AuthenticatedRetrofit") retrofit: Retrofit): SettingsService {
        return retrofit.create(SettingsService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnalyticsService(@Named("AuthenticatedRetrofit") retrofit: Retrofit): AnalyticsService {
        return retrofit.create(AnalyticsService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationsService(@Named("AuthenticatedRetrofit") retrofit: Retrofit): NotificationsService {
        return retrofit.create(NotificationsService::class.java)
    }

    @Provides
    @Singleton
    fun provideParticipantsService(@Named("AuthenticatedRetrofit") retrofit: Retrofit): ParticipantsService {
        return retrofit.create(ParticipantsService::class.java)
    }

    @Provides
    @Singleton
    fun provideChangePasswordAuthService(@Named("AuthenticatedRetrofit") retrofit: Retrofit): ChangePasswordAuthService {
        return retrofit.create(ChangePasswordAuthService::class.java)
    }

    private const val BASE_URL = "http://localhost:8080/"

    // Interceptor для логирования
    private fun getLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }

    private fun getDefaultHeadersInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            val osVersion = Build.VERSION.RELEASE
            val appVersion = "1.0"

            // Получаем существующие ID из заголовков или генерируем новые
            val requestId = originalRequest.header("X-Request-ID") ?: UUID.randomUUID().toString()
            val correlationId = originalRequest.header("X-Correlation-ID") ?: requestId

            val newRequest = originalRequest.newBuilder()
                // Стандартные заголовки
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

                // ID для трейсинга
                .addHeader("X-Request-ID", requestId)
                .addHeader("X-Correlation-ID", correlationId)

                .addHeader("Connection", "keep-alive")
                .addHeader("Keep-Alive", "timeout=30, max=1000")

                // Пользовательский агент
                .addHeader(
                    "User-Agent",
                    "MyBudget/$appVersion ($manufacturer $model; Android $osVersion)"
                )
                .build()

            Timber.i("""
                Sending request:
                URL: ${newRequest.url}
                Headers: ${newRequest.headers}
                Call ID: $requestId
            """.trimIndent())

            chain.proceed(newRequest)
        }
    }
}

