package ru.iuturakulov.mybudget.data.remote.dto

data class OverviewAnalyticsDto(
    val totalAmount: Double,                                 // суммарная сумма по всем проектам
    val categoryDistribution: List<OverviewCategoryStats>,
    val periodDistribution: List<OverviewPeriodStats>,
    val projectComparison: List<ProjectComparisonStats>
)

/** Статистика для одного сегмента диаграммы */
data class OverviewCategoryStats(
    val category: String,    // имя категории
    val amount: Double,      // сумма в этой категории
    val percentage: Double   // % от totalAmount (0..100)
)

/** Статистика для одного шага в bar‑chart по периодам */
data class OverviewPeriodStats(
    val period: String,      // например "2025-04"
    val amount: Double       // сумма за этот период
)

/** Статистика для одного проекта в сравнительном bar‑chart */
data class ProjectComparisonStats(
    val projectId: String,
    val projectName: String,
    val amount: Double       // сумма по этому проекту
)
