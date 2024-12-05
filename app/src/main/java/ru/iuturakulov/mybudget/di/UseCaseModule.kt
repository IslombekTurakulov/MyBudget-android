package ru.iuturakulov.mybudget.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.TransactionRepository
import ru.iuturakulov.mybudget.domain.usecases.AddTransactionUseCase
import ru.iuturakulov.mybudget.domain.usecases.FetchProjectsUseCase

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
    fun provideAddTransactionUseCase(
        transactionRepository: TransactionRepository
    ): AddTransactionUseCase {
        return AddTransactionUseCase(transactionRepository)
    }
}