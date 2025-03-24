package ru.iuturakulov.mybudget.data.remote.dto

data class ProjectAnalyticsDto(
    val projectId: String,                                // ID проекта
    val projectName: String,                          // Название проекта
    val totalAmount: Double,                          // Общая сумма расходов
    val categoryDistribution: List<CategoryDistributionDto>, // Распределение по категориям
    val periodDistribution: List<PeriodDistributionDto>,      // Распределение по периодам
)
