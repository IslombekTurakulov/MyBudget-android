package ru.iuturakulov.mybudget.features.projects.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import ru.iuturakulov.mybudget.core.network.ApiServiceFactory
import ru.iuturakulov.mybudget.features.projects.ProjectApiService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProjectsModule {

//    @Provides
//    @Singleton
//    fun provideProjectRepository(
//        apiService: MainApiService,
//        projectDao: ProjectDao
//    ): ProjectRepository {
//        return ProjectRepositoryImpl(apiService, projectDao)
//    }
}