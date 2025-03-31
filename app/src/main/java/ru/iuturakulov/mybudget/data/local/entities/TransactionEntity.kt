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
    @PrimaryKey(autoGenerate = false) val id: String,
    val projectId: String,
    val name: String,
    val category: String,
    val categoryIcon: String, // URL или имя ресурса
    val amount: Double,
    val userId: String,
    val userName: String,
    val date: Long, // ISO 8601: "YYYY-MM-DD"
    val type: String, // Тип транзакции: "Доход" или "Расход"
    val images: List<String>
) {

    enum class TransactionType(val typeName: String) {
        INCOME("income"),
        EXPENSE("expense");

        companion object {
            fun fromString(value: String): TransactionType {
                return entries.firstOrNull {
                    it.typeName.lowercase().trim() == value
                } ?: TransactionType.INCOME
            }
        }
    }

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
                userName = userName,
                date = date,
                type = TransactionType.fromString(type),
                images = images
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
    val userName: String,
    val date: Long,
    val type: TransactionEntity.TransactionType,           // Тип транзакции: "Доход" или "Расход"
    val images: List<String>    // Список путей/URI изображений транзакции
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
                userName = userName,
                date = date,
                type = type.typeName,
                images = images
            )
        }
    }
}