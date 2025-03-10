package ru.iuturakulov.mybudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.iuturakulov.mybudget.data.local.daos.ParticipantsDao
import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity

@Database(
    entities = [
        ProjectEntity::class,
        TransactionEntity::class,
        ParticipantEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun transactionDao(): TransactionDao
    abstract fun participantsDao(): ParticipantsDao
}