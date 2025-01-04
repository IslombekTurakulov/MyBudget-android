package ru.iuturakulov.mybudget.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.remote.ProjectService
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.TransactionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        projectService: ProjectService,
        transactionDao: TransactionDao,
    ): ProjectRepository {
        return ProjectRepository(projectDao, projectService, transactionDao)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        projectService: ProjectService,
        transactionDao: TransactionDao
    ): TransactionRepository {
        return TransactionRepository(transactionDao, projectService)
    }
}