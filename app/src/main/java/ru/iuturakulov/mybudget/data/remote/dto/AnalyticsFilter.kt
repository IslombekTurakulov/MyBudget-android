package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable

// Модель фильтра аналитики
data class AnalyticsFilter(
    val fromDate: Long? = null,  // Начало периода (timestamp)
    val toDate: Long? = null,    // Конец периода (timestamp)
    val categories: List<String>? = null, // Список категорий для анализа
    val granularity: Granularity = Granularity.MONTH
)

@Serializable
enum class Granularity {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    companion object {
        fun fromFilter(granularity: String): Granularity? {
            return entries.firstOrNull { enum ->
                enum.name.equals(granularity, ignoreCase = true)
            }
        }
    }
}