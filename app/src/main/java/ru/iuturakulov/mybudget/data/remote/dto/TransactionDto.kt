package ru.iuturakulov.mybudget.data.remote.dto

data class TransactionDto(
    val id: String,
    val projectId: String,
    val userId: String,
    val name: String,
    val category: String,
    val categoryIcon: String,
    val amount: Double,
    val date: Long
)