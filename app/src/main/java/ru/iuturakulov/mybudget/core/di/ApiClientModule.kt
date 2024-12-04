package ru.iuturakulov.mybudget.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.iuturakulov.mybudget.features.projects.ProjectApiClient
import ru.iuturakulov.mybudget.features.projects.ProjectApiService
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiClientModule {

    @Provides
    @Singleton
    fun provideProjectApiClient(
        @Named("MainApiService") apiService: ProjectApiService
    ): ProjectApiClient {
        return ProjectApiClient(apiService)
    }

//    @Provides
//    @Singleton
//    fun provideTransactionApiClient(
//        @Named("FeatureApiService") apiService: TransactionApiService
//    ): TransactionApiClient {
//        return TransactionApiClient(apiService)
//    }
}