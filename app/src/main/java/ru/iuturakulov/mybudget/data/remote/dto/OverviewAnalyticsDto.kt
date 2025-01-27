package ru.iuturakulov.mybudget.data.remote.dto

data class OverviewAnalyticsDto(
    val categoryDistribution: List<CategoryDistributionDto>, // Распределение категорий
    val periodDistribution: List<PeriodDistributionDto>,     // Распределение по периодам
    val taskComparison: TaskComparisonDto,                   // Сравнение выполненных и невыполненных задач
    val projectComparison: List<ProjectComparisonDto>        // Сравнение проектов
)

data class CategoryDistributionDto(
    val category: String, // Название категории (например, "Еда")
    val amount: Double    // Сумма расходов в данной категории
)

data class PeriodDistributionDto(
    val period: String,  // Название периода (например, "Январь 2024")
    val amount: Double   // Сумма расходов за данный период
)

data class ProjectComparisonDto(
    val projectName: String, // Название проекта
    val amount: Double       // Сумма расходов по проекту
)

data class TaskComparisonDto(
    val completedTasks: Int,  // Количество выполненных задач
    val totalTasks: Int       // Общее количество задач
)
