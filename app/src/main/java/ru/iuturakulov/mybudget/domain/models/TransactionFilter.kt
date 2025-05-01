package ru.iuturakulov.mybudget.domain.models

import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity

data class TransactionFilter(
    val category: String? = null,
    val type: TransactionEntity.TransactionType? = null,
    val userName: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    var startDate: Long? = null,
    var endDate: Long? = null
)
