package ru.iuturakulov.mybudget.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import ru.iuturakulov.mybudget.data.local.AppDatabase
import ru.iuturakulov.mybudget.data.remote.ApiClient
import ru.iuturakulov.mybudget.data.remote.ProjectService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(@ApplicationContext context: Context): Retrofit {
        return ApiClient.getRetrofitInstance(context)
    }

    @Provides
    @Singleton
    fun provideProjectService(retrofit: Retrofit): ProjectService {
        return retrofit.create(ProjectService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mybudget.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context) =
        EncryptedSharedPreferences.create(
            "secure_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    @Provides
    fun provideProjectDao(database: AppDatabase) = database.projectDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase) = database.transactionDao()
}