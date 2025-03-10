package ru.iuturakulov.mybudget.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.TransactionRepository
import ru.iuturakulov.mybudget.domain.usecases.auth.LoginUseCase
import ru.iuturakulov.mybudget.domain.usecases.project.FetchProjectsUseCase
import ru.iuturakulov.mybudget.domain.usecases.project.SyncProjectsUseCase

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    fun provideFetchProjectsUseCase(
        projectRepository: ProjectRepository
    ): FetchProjectsUseCase {
        return FetchProjectsUseCase(projectRepository)
    }

    @Provides
    fun provideSyncProjectsUseCase(
        projectRepository: ProjectRepository
    ): SyncProjectsUseCase {
        return SyncProjectsUseCase(projectRepository)
    }

    @Provides
    fun provideLoginUseCase(
        authService: AuthService,
        tokenStorage: TokenStorage
    ): LoginUseCase {
        return LoginUseCase(authService, tokenStorage)
    }
}