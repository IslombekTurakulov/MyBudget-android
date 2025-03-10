package ru.iuturakulov.mybudget.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import ru.iuturakulov.mybudget.domain.repositories.AnalyticsRepository
import javax.inject.Inject

@HiltViewModel
class ProjectAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ProjectAnalyticsDto>>(UiState.Idle)
    val uiState: StateFlow<UiState<ProjectAnalyticsDto>> = _uiState.asStateFlow()

    /**
     * Загрузка аналитики проекта.
     */
    fun loadProjectAnalytics(projectId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val analytics = analyticsRepository.getProjectAnalytics(projectId).body()
                _uiState.value = UiState.Success(analytics)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка загрузки аналитики")
            }
        }
    }
}
