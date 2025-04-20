package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class AnalyticsExportFormat {
    PDF,
    CSV;
}
