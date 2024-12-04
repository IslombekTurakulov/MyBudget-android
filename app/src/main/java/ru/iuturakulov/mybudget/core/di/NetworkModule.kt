package ru.iuturakulov.mybudget.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.iuturakulov.mybudget.core.network.ApiServiceFactory
import ru.iuturakulov.mybudget.features.projects.ProjectApiService
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://localhost:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .client(provideOkHttpClient())
        .build()

    @Provides
    @Singleton
    @Named("ProjectApiService")
    fun provideMainApiService(
        retrofitBuilder: Retrofit.Builder
    ): ProjectApiService {
        return ApiServiceFactory.createService(
            retrofitBuilder, "https://localhost:8080/main"
        )
    }

    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(1000, TimeUnit.SECONDS)
            .readTimeout(1000, TimeUnit.SECONDS)
            .build()
    }
}