package ru.iuturakulov.mybudget.data.remote.dto

data class OverviewAnalyticsDto(
    val categoryDistribution: List<CategoryDistributionDto>, // Распределение категорий
    val periodDistribution: List<PeriodDistributionDto>,     // Распределение по периодам
    val projectComparison: List<ProjectComparisonDto>        // Сравнение проектов
)

data class CategoryDistributionDto(
    val category: String, // Название категории (например, "Еда")
    val totalAmount: Double    // Сумма расходов в данной категории
)

data class PeriodDistributionDto(
    val period: String,  // Название периода (например, "2025-03-20")
    val totalAmount: Double   // Сумма расходов за данный период
)

data class ProjectComparisonDto(
    val projectName: String, // Название проекта
    val totalSpent: Double    // Сумма расходов по проекту
)

