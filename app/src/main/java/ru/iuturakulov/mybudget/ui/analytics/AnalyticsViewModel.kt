package ru.iuturakulov.mybudget.ui.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsExportFormat
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsExportFrom
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsFilter
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import ru.iuturakulov.mybudget.domain.repositories.AnalyticsRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _overviewAnalytics =
        MutableStateFlow<UiState<OverviewAnalyticsDto>>(UiState.Idle)
    val overviewAnalytics: StateFlow<UiState<OverviewAnalyticsDto>> = _overviewAnalytics

    private val _projectAnalytics =
        MutableStateFlow<UiState<ProjectAnalyticsDto>>(UiState.Idle)
    val projectAnalytics: StateFlow<UiState<ProjectAnalyticsDto>> = _projectAnalytics

    private val _filter = MutableStateFlow(AnalyticsFilter())
    val filter: StateFlow<AnalyticsFilter> = _filter

    private val _exportState = MutableSharedFlow<UiState<File>>(replay = 0)
    val exportState: SharedFlow<UiState<File>> = _exportState

    private var currentProjectId: String? = null   // null → overview

    init {
        viewModelScope.launch {
            filter.drop(1).collect { currentProjectId?.let { fetchProjectAnalytics(it) } }
        }
        fetchOverviewAnalytics()       // первый запрос
    }

    fun applyFilter(newFilter: AnalyticsFilter) {
        _filter.value = newFilter
    }

    fun fetchOverviewAnalytics() = viewModelScope.launch {
        _overviewAnalytics.value = UiState.Loading
        try {
            val dto = analyticsRepository
                .getOverviewAnalytics(_filter.value)
                .body()
            _overviewAnalytics.value = UiState.Success(dto)
        } catch (e: IOException) {
            _overviewAnalytics.value = UiState.Error("Ошибка сети")
        } catch (e: Exception) {
            _overviewAnalytics.value = UiState.Error(e.localizedMessage ?: "")
        }
    }

    fun fetchProjectAnalytics(projectId: String) {
        currentProjectId = projectId
        viewModelScope.launch {
            _projectAnalytics.value = UiState.Loading
            try {
                val dto = analyticsRepository
                    .getProjectAnalytics(projectId, _filter.value)
                    .body()
                _projectAnalytics.value = UiState.Success(dto)
            } catch (e: IOException) {
                _projectAnalytics.value = UiState.Error("Ошибка сети")
            } catch (e: Exception) {
                _projectAnalytics.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    fun exportAnalytics(format: AnalyticsExportFormat) = viewModelScope.launch {
        val from = if (currentProjectId == null)
            AnalyticsExportFrom.OVERVIEW else AnalyticsExportFrom.PROJECT

        _exportState.emit(UiState.Loading)

        try {
            val file = analyticsRepository.exportAnalytics(
                exportFrom = from,
                projectId = currentProjectId,
                filter = _filter.value,
                format = format
            )
            _exportState.emit(UiState.Success(file))
        } catch (e: HttpException) {
            _exportState.emit(UiState.Error("Сервер вернул ошибку: ${e.code()}"))
        } catch (e: IOException) {
            _exportState.emit(UiState.Error("Проблемы с сетью"))
        } catch (e: Exception) {
            _exportState.emit(UiState.Error(e.localizedMessage ?: "Неизвестная ошибка"))
        }
    }
}
