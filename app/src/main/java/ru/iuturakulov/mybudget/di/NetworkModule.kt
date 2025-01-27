package ru.iuturakulov.mybudget.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.AnalyticsService
import ru.iuturakulov.mybudget.data.remote.ApiClient
import ru.iuturakulov.mybudget.data.remote.AuthService
import ru.iuturakulov.mybudget.data.remote.ParticipantsService
import ru.iuturakulov.mybudget.data.remote.ProjectService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(tokenStorage: TokenStorage): Retrofit {
        return ApiClient.getRetrofitInstance(tokenStorage)
    }

    @Provides
    @Singleton
    fun provideTokenStorage(sharedPreferences: SharedPreferences): TokenStorage {
        return TokenStorage(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnalyticsService(retrofit: Retrofit): AnalyticsService {
        return retrofit.create(AnalyticsService::class.java)
    }

    @Provides
    @Singleton
    fun provideParticipantsService(retrofit: Retrofit): ParticipantsService {
        return retrofit.create(ParticipantsService::class.java)
    }

    @Provides
    @Singleton
    fun provideProjectService(retrofit: Retrofit): ProjectService {
        return retrofit.create(ProjectService::class.java)
    }
}