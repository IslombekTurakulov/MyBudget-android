package ru.iuturakulov.mybudget.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
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

    private val _appliedFilter = MutableStateFlow(AnalyticsFilter())
    val appliedFilter: StateFlow<AnalyticsFilter> = _appliedFilter

    private val _initialOverviewAnalytics =
        MutableStateFlow<UiState<OverviewAnalyticsDto>>(UiState.Idle)
    val initialOverviewAnalytics: StateFlow<UiState<OverviewAnalyticsDto>> =
        _initialOverviewAnalytics

    // Отфильтрованная overview (с учётом appliedFilter)
    private val _filteredOverviewAnalytics =
        MutableStateFlow<UiState<OverviewAnalyticsDto>>(UiState.Idle)
    val filteredOverviewAnalytics: StateFlow<UiState<OverviewAnalyticsDto>> =
        _filteredOverviewAnalytics

    // Изначальная и отфильтрованная project
    private val _initialProjectAnalytics =
        MutableStateFlow<UiState<ProjectAnalyticsDto>>(UiState.Idle)
    val initialProjectAnalytics: StateFlow<UiState<ProjectAnalyticsDto>> =
        _initialProjectAnalytics

    private val _filteredProjectAnalytics =
        MutableStateFlow<UiState<ProjectAnalyticsDto>>(UiState.Idle)
    val filteredProjectAnalytics: StateFlow<UiState<ProjectAnalyticsDto>> =
        _filteredProjectAnalytics

    private var currentProjectId: String? = null

    private val _exportState = MutableSharedFlow<UiState<File>>(replay = 0)
    val exportState: SharedFlow<UiState<File>> = _exportState

    private var _currentPeriodLabels: List<String> = emptyList()
    val currentPeriodLabels: List<String> get() = _currentPeriodLabels

    init {
        fetchInitialOverviewAnalytics()
        fetchFilteredOverviewAnalytics()

        viewModelScope.launch {
            _appliedFilter.drop(1).collect {
                fetchFilteredOverviewAnalytics()
                currentProjectId?.let { fetchFilteredProjectAnalytics(it) }
            }
        }
    }

    fun applyFilter(newFilter: AnalyticsFilter) {
        _appliedFilter.value = newFilter
    }

    private fun fetchInitialOverviewAnalytics() = viewModelScope.launch {
        _initialOverviewAnalytics.value = UiState.Loading
        try {
            val dto = analyticsRepository
                .getOverviewAnalytics(AnalyticsFilter())   // без фильтра
                .body()!!
            _initialOverviewAnalytics.value = UiState.Success(dto)

            updatePeriodLabels(dto.periodDistribution.map { it.period })
        } catch (e: Exception) {
            _initialOverviewAnalytics.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun fetchFilteredOverviewAnalytics() = viewModelScope.launch {
        _filteredOverviewAnalytics.value = UiState.Loading
        try {
            val dto = analyticsRepository
                .getOverviewAnalytics(_appliedFilter.value) // с текущим фильтром
                .body()!!
            _filteredOverviewAnalytics.value = UiState.Success(dto)

            updatePeriodLabels(dto.periodDistribution.map { it.period })
        } catch (e: Exception) {
            _filteredOverviewAnalytics.value = UiState.Error(e.message ?: "Error")
        }
    }

    // --- Project ---

    /** Вызывать из UI, когда нужно открыть конкретный проект */
    fun startProjectAnalytics(projectId: String) {
        currentProjectId = projectId
        fetchInitialProjectAnalytics(projectId)
        fetchFilteredProjectAnalytics(projectId)
    }

    fun startOverviewProjectAnalytics() {
        fetchInitialOverviewAnalytics()
        fetchFilteredOverviewAnalytics()
    }

    private fun fetchInitialProjectAnalytics(projectId: String) = viewModelScope.launch {
        _initialProjectAnalytics.value = UiState.Loading
        try {
            val dto = analyticsRepository
                .getProjectAnalytics(projectId, AnalyticsFilter())
                .body()!!
            _initialProjectAnalytics.value = UiState.Success(dto)
        } catch (e: Exception) {
            _initialProjectAnalytics.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun fetchFilteredProjectAnalytics(projectId: String) = viewModelScope.launch {
        _filteredProjectAnalytics.value = UiState.Loading
        try {
            val dto = analyticsRepository
                .getProjectAnalytics(projectId, _appliedFilter.value)
                .body()!!
            _filteredProjectAnalytics.value = UiState.Success(dto)
        } catch (e: Exception) {
            _filteredProjectAnalytics.value = UiState.Error(e.message ?: "Error")
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
                filter = _appliedFilter.value,
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

    /**
     * Сохраняет список меток периодов для BarChart drill-down.
     */
    fun updatePeriodLabels(labels: List<String>) {
        _currentPeriodLabels = labels
    }
}
