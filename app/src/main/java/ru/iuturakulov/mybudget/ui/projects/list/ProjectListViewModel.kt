package ru.iuturakulov.mybudget.ui.projects.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.domain.models.UserSettings
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.SettingsRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ProjectEntity>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ProjectEntity>>> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(ProjectStatus.ALL)
    val filterStatus: SharedFlow<ProjectStatus> = _filterStatus
    // Оригинальный MutableStateFlow для обновления значений
    private val _projectsRaw = MutableStateFlow<List<ProjectEntity>>(emptyList())

    // Публичная версия с поддержкой stateIn
    private val _projects = _projectsRaw
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = _projects

    private val _syncEvent = MutableSharedFlow<Boolean>()
    val syncEvent: SharedFlow<Boolean> = _syncEvent
    private val _inviteCodeEvent = MutableSharedFlow<UiState<String>>()
    val inviteCodeEvent: SharedFlow<UiState<String>> = _inviteCodeEvent
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        _projects,
        _searchQuery,
        _filterStatus
    ) { projects, query, status ->
        projects
            .filterByQuery(query)
            .filterByStatus(status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val projects = projectRepository.getProjectsFlow().first()
                _projectsRaw.value = projects // Обновляем raw-версию
                _uiState.value = UiState.Success(projects)
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is IOException -> "Нет подключения к сети"
                    else -> e.localizedMessage ?: "Ошибка загрузки"
                }
                _uiState.value = UiState.Error(errorMsg)
            }
        }
    }

    /**
     * Синхронизирует проекты с сервером.
     */
    fun syncProjects() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            _uiState.value = UiState.Loading

            try {
                val projects = projectRepository.syncProjects().first()
                _projectsRaw.value = projects // Обновляем raw-версию
                _syncEvent.emit(true)
                _uiState.value = UiState.Success(projects)
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is IOException -> "Ошибка сети при синхронизации"
                    else -> e.localizedMessage ?: "Ошибка синхронизации"
                }
                _uiState.value = UiState.Error(errorMsg)
                _syncEvent.emit(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: ProjectStatus) {
        _filterStatus.value = status
    }

    fun joinProjectByCode(code: String) {
        viewModelScope.launch {
            _inviteCodeEvent.emit(UiState.Loading)
            try {
                val result = projectRepository.getProjectByInviteCode(code)
                if (result) {
                    syncProjects()
                    _inviteCodeEvent.emit(UiState.Success("Вы успешно присоединились к проекту!"))
                } else {
                    throw Exception("Неверный код приглашения")
                }
            } catch (e: Exception) {
                _inviteCodeEvent.emit(UiState.Error("Ошибка: ${e.localizedMessage ?: "неизвестная ошибка"}"))
            }
        }
    }

    private fun List<ProjectEntity>.filterByQuery(query: String): List<ProjectEntity> {
        if (query.isEmpty()) return this
        return filter { project ->
            project.name.contains(query, ignoreCase = true) ||
                    project.description?.contains(query, ignoreCase = true) == true
        }
    }

    private fun List<ProjectEntity>.filterByStatus(status: ProjectStatus): List<ProjectEntity> {
        if (status == ProjectStatus.ALL) return this
        return filter { status.canTransitionTo(it.status) }
    }
}