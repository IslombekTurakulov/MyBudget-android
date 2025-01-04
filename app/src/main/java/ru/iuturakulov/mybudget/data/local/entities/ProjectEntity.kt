package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val name: String,
    val description: String,
    val budgetLimit: Double,
    val amountSpent: Double,
    val status: ProjectStatus, // "Активен", "Завершён" TODO: ENUM?
    val createdDate: String,
    val lastModified: String // Для отслеживания изменений
)


