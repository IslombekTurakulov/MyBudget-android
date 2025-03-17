package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity.TransactionType

@Serializable
data class TransactionDto(
    val id: String,
    val projectId: String,
    val userId: String,
    val name: String,
    val category: String,
    val categoryIcon: String,
    val amount: Double,
    val date: Long,
    val transactionType: TransactionType = TransactionType.INCOME,
    val images: List<String> = emptyList()
)