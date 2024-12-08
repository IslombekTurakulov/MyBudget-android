package ru.iuturakulov.mybudget.domain.models

data class TransactionFilter(
    val category: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)
