package ru.iuturakulov.mybudget.data.remote.dto

data class TransactionInfo(
    val id: String,
    val projectName: String? = null,
    val projectId: String? = null,
    val name: String,
    val amount: Double,
    val date: String,
    val userName: String,
    val type: String,
    val categoryIcon: String?
)