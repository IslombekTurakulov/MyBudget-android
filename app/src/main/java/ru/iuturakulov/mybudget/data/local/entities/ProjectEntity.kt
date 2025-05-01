package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "budget_limit") val budgetLimit: Double,
    @ColumnInfo(name = "amount_spent") val amountSpent: Double,
    @ColumnInfo(name = "status") val status: ProjectStatus,
    @ColumnInfo(name = "created_date") val createdAt: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,

    // -- migrartion 1.2 ---
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "category_icon") val categoryIcon: String?,

    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "owner_name") val ownerName: String,
    @ColumnInfo(name = "owner_email") val ownerEmail: String
)



