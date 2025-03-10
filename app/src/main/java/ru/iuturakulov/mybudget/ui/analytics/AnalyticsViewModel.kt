package ru.iuturakulov.mybudget.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
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
    val overviewAnalytics: StateFlow<UiState<OverviewAnalyticsDto>> =
        _overviewAnalytics.asStateFlow()

    private val _projectAnalytics = MutableStateFlow<UiState<ProjectAnalyticsDto>>(UiState.Idle)
    val projectAnalytics: StateFlow<UiState<ProjectAnalyticsDto>> = _projectAnalytics.asStateFlow()

    // Фильтры для аналитики
    private val _filters = MutableStateFlow(AnalyticsFilter())
    val filters: StateFlow<AnalyticsFilter> = _filters.asStateFlow()

    init {
        observeFilters()
    }

    // Загрузка общей аналитики
    fun loadOverviewAnalytics() {
        viewModelScope.launch {
            _overviewAnalytics.value = UiState.Loading
            try {
                val analytics = analyticsRepository.getOverviewAnalytics().body()
                _overviewAnalytics.value = UiState.Success(analytics)
            } catch (e: IOException) {
                _overviewAnalytics.value = UiState.Error("Ошибка сети. Проверьте соединение.")
            } catch (e: Exception) {
                _overviewAnalytics.value =
                    UiState.Error(e.localizedMessage ?: "Ошибка загрузки данных")
            }
        }
    }

    // Загрузка аналитики конкретного проекта
    fun loadProjectAnalytics(projectId: String) {
        viewModelScope.launch {
            _projectAnalytics.value = UiState.Loading
            try {
                val analytics = analyticsRepository.getProjectAnalytics(projectId).body()
                _projectAnalytics.value = UiState.Success(analytics)
            } catch (e: IOException) {
                _projectAnalytics.value = UiState.Error("Ошибка сети. Проверьте соединение.")
            } catch (e: Exception) {
                _projectAnalytics.value =
                    UiState.Error(e.localizedMessage ?: "Ошибка загрузки данных")
            }
        }
    }

    // Применение фильтров
    fun applyFilters(newFilters: AnalyticsFilter) {
        _filters.value = newFilters
    }

    private fun observeFilters() {
        viewModelScope.launch {
            _filters.collect { filters ->
                // Применение фильтров при изменении
                if (_projectAnalytics.value is UiState.Success) {
                    val currentData = (_projectAnalytics.value as UiState.Success).data
                    val filteredData = currentData?.let { filterAnalytics(it, filters) }
                    _projectAnalytics.value = UiState.Success(filteredData)
                }
            }
        }
    }

    private fun filterAnalytics(
        data: ProjectAnalyticsDto,
        filters: AnalyticsFilter
    ): ProjectAnalyticsDto {
        return data.copy(
            categoryDistribution = data.categoryDistribution.filter { it.category in filters.categories },
            periodDistribution = data.periodDistribution.filter { it.period in filters.periods }
        )
    }
}

// Модель фильтра аналитики
data class AnalyticsFilter(
    val categories: List<String> = emptyList(),
    val periods: List<String> = emptyList()
)
