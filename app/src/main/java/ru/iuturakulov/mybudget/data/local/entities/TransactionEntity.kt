package ru.iuturakulov.mybudget.data.local.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class TransactionEntity(
    // TODO: отказ от автогенерации String(projectId-userId-uuid)
    @PrimaryKey(autoGenerate = false) val id: String,
    val projectId: String,
    val name: String,
    val category: String,
    val categoryIcon: String, // URL или имя ресурса
    val amount: Double,
    val userId: String,
    val date: Long // ISO 8601: "YYYY-MM-DD"
) {

    companion object {
        fun TransactionEntity.toTemporaryModel(): TemporaryTransaction {
            return TemporaryTransaction(
                id = id,
                projectId = projectId,
                name = name,
                amount = amount,
                category = category,
                categoryIcon = categoryIcon,
                userId = userId,
                date = date
            )
        }
    }
}

@Parcelize
data class TemporaryTransaction(
    val id: String,
    val projectId: String,
    val name: String,
    val amount: Double,
    val category: String,
    val categoryIcon: String,
    val userId: String,
    val date: Long,
) : Parcelable {
    companion object {
        fun TemporaryTransaction.toEntity(): TransactionEntity {
            return TransactionEntity(
                id = id,
                projectId = projectId,
                name = name,
                amount = amount,
                category = category,
                categoryIcon = categoryIcon,
                userId = userId,
                date = date
            )
        }
    }
}