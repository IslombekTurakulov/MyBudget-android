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
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ProjectEntity>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ProjectEntity>>> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(ProjectStatus.ALL)
    val filterStatus: SharedFlow<ProjectStatus> = _filterStatus
    private val _projects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    private val _syncEvent = MutableSharedFlow<Boolean>()
    val syncEvent: SharedFlow<Boolean> = _syncEvent
    private val _inviteCodeEvent = MutableSharedFlow<UiState<String>>()
    val inviteCodeEvent: SharedFlow<UiState<String>> = _inviteCodeEvent

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        _projects,
        _searchQuery,
        _filterStatus
    ) { projects, query, status ->
        projects.filter { project ->
            (query.isEmpty() || project.name.contains(
                query,
                ignoreCase = true
            ) || project.description?.contains(query, ignoreCase = true) == true) &&
                    (status == ProjectStatus.ALL || status.canTransitionTo(project.status))
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        syncProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val projects = projectRepository.getProjectsFlow().first()
                _projects.value = projects
                _uiState.value = UiState.Success(projects)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка загрузки")
            }
        }
    }

    fun syncProjects() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val projects = projectRepository.syncProjects().first()
                _projects.value = projects
                _syncEvent.emit(true)
                _uiState.value = UiState.Success(projects)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка загрузки")
                _syncEvent.emit(false)
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
            try {
                val result = projectRepository.getProjectByInviteCode(code)
                if (result) {
                    syncProjects()
                    _inviteCodeEvent.emit(UiState.Success("Вы успешно присоединились к проекту!"))
                } else {
                    throw Exception()
                }
            } catch (e: Exception) {
                _inviteCodeEvent.emit(UiState.Error("Ошибка присоединения: ${e.localizedMessage}"))
            }
        }
    }
}
