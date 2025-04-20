package ru.iuturakulov.mybudget.domain.repositories

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import ru.iuturakulov.mybudget.data.remote.AnalyticsService
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsExportFormat
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsExportFrom
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsFilter
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val analyticsService: AnalyticsService,
    @ApplicationContext private val context: Context
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


    suspend fun exportAnalytics(
        exportFrom: AnalyticsExportFrom,       // OVERVIEW / PROJECT
        projectId: String? = null,  // обязательный при PROJECT
        filter: AnalyticsFilter,
        format: AnalyticsExportFormat      // CSV / PDF
    ): File = withContext(Dispatchers.IO) {

        val response = analyticsService.fetchExportAnalytics(
            format = format,
            from = exportFrom,
            projectId = projectId,
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            categories = filter.categories,
            granularity = filter.granularity
        )

        if (!response.isSuccessful) throw HttpException(response)

        val body = response.body() ?: throw IOException("Empty response body")

        // ─── сохраняем файл в /cache/exports ───────────────────────────────
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = buildFileName(projectId, exportFrom, format, filter)
        val file = File(dir, fileName)

        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file            // <- возвращаем готовый File
    }

    /* ---------- helpers ---------- */

    private fun buildFileName(
        projectId: String?,
        from: AnalyticsExportFrom,
        format: AnalyticsExportFormat,
        filter: AnalyticsFilter
    ): String {
        val period = listOfNotNull(filter.fromDate, filter.toDate)
            .joinToString("_") { it.toString() }
            .ifBlank { "all_time" }

        return when (from) {
            AnalyticsExportFrom.OVERVIEW ->
                "overview_$period.${format.name.lowercase()}"

            AnalyticsExportFrom.PROJECT ->
                "project_${projectId ?: "unknown"}_$period.${format.name.lowercase()}"
        }
    }
}

