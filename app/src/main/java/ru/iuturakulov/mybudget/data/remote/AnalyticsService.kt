package ru.iuturakulov.mybudget.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.iuturakulov.mybudget.data.remote.dto.Granularity
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto

interface AnalyticsService {
    @GET("analytics/overview")
    suspend fun fetchOverviewAnalytics(
        @Query("fromDate") fromDate: Long?,
        @Query("toDate") toDate: Long?,
        @Query("categories") categories: List<String>?,
        @Query("granularity") granularity: Granularity?
    ): Response<OverviewAnalyticsDto>

    @GET("analytics/project/{projectId}")
    suspend fun fetchProjectAnalytics(
        @Path("projectId") projectId: String,
        @Query("fromDate") fromDate: Long?,
        @Query("toDate") toDate: Long?,
        @Query("categories") categories: List<String>?,
        @Query("granularity") granularity: Granularity?
    ): Response<ProjectAnalyticsDto>
}
