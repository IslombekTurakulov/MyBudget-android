package ru.iuturakulov.mybudget.data.remote.dto

import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity

/**
 * Детальная аналитика по конкретному проекту
 */
data class ProjectAnalyticsDto(
    val projectId: String,
    val projectName: String,
    val totalAmount: Double,
    val categoryDistribution: List<CategoryStats>,
    val periodDistribution: List<PeriodStats>
)

/** Статистика по категориям в рамках одного проекта */
data class CategoryStats(
    val category: String,      // имя категории
    val amount: Double,        // сумма
    val percentage: Double,     // % от totalAmount (0..100),
    val transactionInfo: List<TransactionInfo>? = null,
)

/** Статистика по периодам (месяцам) в рамках одного проекта */
data class PeriodStats(
    val period: String,
    val totalAmount: Double    // сумма
)
