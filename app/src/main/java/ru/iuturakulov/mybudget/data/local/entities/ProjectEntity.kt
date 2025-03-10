package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = false) val id: String, // Уникальный идентификатор
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "budget_limit") val budgetLimit: Double,
    @ColumnInfo(name = "amount_spent") val amountSpent: Double,
    @ColumnInfo(name = "status") val status: ProjectStatus,
    @ColumnInfo(name = "created_date") val createdDate: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long
)



