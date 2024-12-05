package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val amount: Double,
    val type: String,
    val date: String,
    val description: String?
)