package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto

interface AnalyticsService {

    @GET("analytics/overview")
    suspend fun fetchOverviewAnalytics(): OverviewAnalyticsDto

    @GET("analytics/project/{projectId}")
    suspend fun fetchProjectAnalytics(@Path("projectId") projectId: String): ProjectAnalyticsDto
}
