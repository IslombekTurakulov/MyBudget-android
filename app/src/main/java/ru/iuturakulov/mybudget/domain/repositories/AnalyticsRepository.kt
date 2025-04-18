package ru.iuturakulov.mybudget.domain.repositories

import retrofit2.Response
import ru.iuturakulov.mybudget.data.remote.AnalyticsService
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsFilter
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val analyticsService: AnalyticsService
) {

    suspend fun getOverviewAnalytics(
        filter: AnalyticsFilter
    ): Response<OverviewAnalyticsDto> =
        analyticsService.fetchOverviewAnalytics(
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            categories = filter.categories,
            granularity = filter.granularity
        )

    suspend fun getProjectAnalytics(
        projectId: String,
        filter: AnalyticsFilter
    ): Response<ProjectAnalyticsDto> =
        analyticsService.fetchProjectAnalytics(
            projectId = projectId,
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            categories = filter.categories,
            granularity = filter.granularity
        )
}

