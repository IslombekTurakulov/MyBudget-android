package ru.iuturakulov.mybudget.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.iuturakulov.mybudget.data.local.AppDatabase
import ru.iuturakulov.mybudget.data.local.daos.NotificationsDao
import ru.iuturakulov.mybudget.data.local.daos.ParticipantsDao
import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.daos.UserSettingsDao
import ru.iuturakulov.mybudget.data.remote.AnalyticsService
import ru.iuturakulov.mybudget.data.remote.FCMService
import ru.iuturakulov.mybudget.data.remote.NotificationsService
import ru.iuturakulov.mybudget.data.remote.ParticipantsService
import ru.iuturakulov.mybudget.data.remote.ProjectService
import ru.iuturakulov.mybudget.data.remote.SettingsService
import ru.iuturakulov.mybudget.domain.repositories.AnalyticsRepository
import ru.iuturakulov.mybudget.domain.repositories.NotificationsRepository
import ru.iuturakulov.mybudget.domain.repositories.ParticipantsRepository
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.SettingsRepository
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

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        analyticsService: AnalyticsService,
        @ApplicationContext context: Context,
    ): AnalyticsRepository {
        return AnalyticsRepository(analyticsService, context)
    }

    @Provides
    @Singleton
    fun provideParticipantsRepository(
        participantsDao: ParticipantsDao,
        participantsService: ParticipantsService
    ): ParticipantsRepository {
        return ParticipantsRepository(participantsDao, participantsService)
    }

    @Provides
    @Singleton
    fun provideNotificationsRepository(
        notificationsDao: NotificationsDao,
        notificationsService: NotificationsService,
        fcmService: FCMService,
    ): NotificationsRepository {
        return NotificationsRepository(notificationsService, notificationsDao, fcmService)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        userSettingsDao: UserSettingsDao,
        settingsService: SettingsService,
        appDatabase: AppDatabase,
    ): SettingsRepository {
        return SettingsRepository(settingsService, userSettingsDao, appDatabase)
    }
}