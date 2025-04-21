package ru.iuturakulov.mybudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.iuturakulov.mybudget.data.local.daos.NotificationsDao
import ru.iuturakulov.mybudget.data.local.daos.ParticipantsDao
import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.daos.UserSettingsDao
import ru.iuturakulov.mybudget.data.local.entities.NotificationEntity
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.local.entities.UserSettingsEntity

@Database(
    entities = [
        ProjectEntity::class,
        TransactionEntity::class,
        ParticipantEntity::class,
        UserSettingsEntity::class,
        NotificationEntity::class,
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun transactionDao(): TransactionDao
    abstract fun participantsDao(): ParticipantsDao
    abstract fun notificationsDao(): NotificationsDao
    abstract fun userSettingsDao(): UserSettingsDao
}
