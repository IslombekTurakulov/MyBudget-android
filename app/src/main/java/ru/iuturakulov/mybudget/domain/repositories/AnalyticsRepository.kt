package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.remote.AnalyticsService
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val analyticsService: AnalyticsService
) {

    suspend fun getOverviewAnalytics(): OverviewAnalyticsDto {
        return analyticsService.fetchOverviewAnalytics()
    }

    suspend fun getProjectAnalytics(projectId: String): ProjectAnalyticsDto {
        return analyticsService.fetchProjectAnalytics(projectId)
    }
}
