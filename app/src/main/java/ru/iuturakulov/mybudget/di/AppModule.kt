package ru.iuturakulov.mybudget.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.iuturakulov.mybudget.data.local.AppDatabase
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN category TEXT")
            db.execSQL("ALTER TABLE projects ADD COLUMN category_icon TEXT")
            db.execSQL("ALTER TABLE projects ADD COLUMN owner_id TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE projects ADD COLUMN owner_name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE projects ADD COLUMN owner_email TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mybudget.db"
        ).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideBaseUrl(
        prefs: SharedPreferences
    ): String {
        val host = prefs.getString(API_HOST_PREF, DEFAULT_HOST) ?: DEFAULT_HOST
        return if (host.endsWith("/")) host else "$host/"
    }

    @Provides
    @Singleton
    @Named("BaseUrl")
    fun provideBaseUrlNamed(prefs: SharedPreferences): String =
        provideBaseUrl(prefs)


    private const val API_HOST_PREF = "api_host"
    private const val DEFAULT_HOST = "http://51.250.65.154/"

    @Provides
    fun provideProjectDao(database: AppDatabase) = database.projectDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase) = database.transactionDao()

    @Provides
    fun provideParticipantsDao(database: AppDatabase) = database.participantsDao()

    @Provides
    fun provideUserSettingsDao(database: AppDatabase) = database.userSettingsDao()

    @Provides
    fun provideNotificationsDao(database: AppDatabase) = database.notificationsDao()
}