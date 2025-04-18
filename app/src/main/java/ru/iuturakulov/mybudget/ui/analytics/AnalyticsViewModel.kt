package ru.iuturakulov.mybudget.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsFilter
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import ru.iuturakulov.mybudget.domain.repositories.AnalyticsRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _overviewAnalytics = MutableStateFlow<UiState<OverviewAnalyticsDto>>(UiState.Idle)
    val overviewAnalytics: StateFlow<UiState<OverviewAnalyticsDto>> = _overviewAnalytics

    private val _projectAnalytics = MutableStateFlow<UiState<ProjectAnalyticsDto>>(UiState.Idle)
    val projectAnalytics: StateFlow<UiState<ProjectAnalyticsDto>> = _projectAnalytics

    private val _filter = MutableStateFlow(AnalyticsFilter())
    val filter: StateFlow<AnalyticsFilter> = _filter

    private var currentProjectId: String? = null

    init {
        viewModelScope.launch {
            filter
                .drop(1)
                .collect { fetchProjectAnalytics(currentProjectId ?: return@collect) }
        }
        fetchOverviewAnalytics()
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
}

