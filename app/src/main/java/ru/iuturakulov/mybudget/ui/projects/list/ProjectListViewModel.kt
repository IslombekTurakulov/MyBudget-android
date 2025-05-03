package ru.iuturakulov.mybudget.ui.projects.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.apache.commons.text.similarity.LevenshteinDistance
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.domain.models.ProjectFilter
import ru.iuturakulov.mybudget.domain.models.UserSettings
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.SettingsRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    // TODO: удалить, не нужен
    private val _uiState = MutableStateFlow<UiState<List<ProjectEntity>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ProjectEntity>>> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _projectsRaw = MutableStateFlow<List<ProjectEntity>>(emptyList())

    // Публичная версия с поддержкой stateIn
    private val _projects = _projectsRaw
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = _projects

    private val _syncEvent = MutableSharedFlow<Boolean>()
    val syncEvent: SharedFlow<Boolean> = _syncEvent
    private val _inviteCodeEvent = MutableSharedFlow<UiState<String>>()
    val inviteCodeEvent: SharedFlow<UiState<String>> = _inviteCodeEvent
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _filterStatuses = MutableStateFlow<ProjectFilter>(
        ProjectFilter()
    )
    val projectFilter: StateFlow<ProjectFilter> = _filterStatuses

    val filteredProjects =
        combine(_projects, _searchQuery, _filterStatuses) { projects, query, filter ->
            projects
                .fuzzyFilterQuery(query)
                .filterByStatuses(filter.statuses)
                .filterByCategory(filter.category)
                .filterByOwner(filter.ownerName)
                .filterByBudget(filter.minBudget, filter.maxBudget)
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(1_000),
            emptyList()
        )

    init {
        loadProjects()
    }

    fun loadProjects() {
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

    private val jw = JaroWinklerSimilarity()

    /**
     * Быстрая фильтрация: проверяет, что все символы query
     * встречаются в строке в том же порядке (subsequence match).
     */
    private fun String.subsequenceMatch(query: String): Boolean {
        if (query.isEmpty()) return true
        var i = 0
        for (ch in this) {
            if (ch.equals(query[i], ignoreCase = true)) {
                i++
                if (i == query.length) return true
            }
        }
        return false
    }

    /** Возвращает меру похожести (0.0–1.0) по алгоритму Jaro–Winkler */
    private fun String.similarityTo(other: String): Double =
        jw.apply(this, other)

    /**
     * Двухэтапный fuzzy-фильтр:
     * 1) subsequenceMatch — быстрый отбор кандидатов,
     * 2) Jaro–Winkler — точная проверка по порогам.
     */
    fun List<ProjectEntity>.fuzzyFilterQuery(
        query: String,
        thresholdTitle: Double = 0.50,
        thresholdDesc: Double = 0.70,
    ): List<ProjectEntity> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return this

        // Быстрый subsequence-фильтр
        val quickCandidates = filter { project ->
            project.name.lowercase().subsequenceMatch(q) ||
                    (project.description?.lowercase()?.subsequenceMatch(q) ?: false)
        }

        // подходит ли проект по порогам
        fun ProjectEntity.matchesBySimilarity(): Boolean {
            // title: любой “кусок” (слово) должен быть похож
            val titleOk = name
                .lowercase()
                .split("\\s+".toRegex())
                .any { it.similarityTo(q) >= thresholdTitle }

            // description: проверяем по словам, а не по всей строке
            val descOk = description
                ?.lowercase()
                ?.split("\\s+".toRegex())
                ?.any { it.similarityTo(q) >= thresholdDesc }
                ?: false

            return titleOk || descOk
        }

        // Применяем Jaro–Winkler к быстрым кандидатам
        val refined = quickCandidates.filter { it.matchesBySimilarity() }
        if (refined.isNotEmpty()) return refined

        // Если в быстрых не нашлось — пробуем по всей коллекции
        return filter { it.matchesBySimilarity() }
    }

    fun setProjectFilter(filter: ProjectFilter) {
        _filterStatuses.value = filter
    }

    private fun List<ProjectEntity>.filterByCategory(cat: String?) =
        if (cat.isNullOrBlank()) this else filter { it.category.equals(cat, ignoreCase = true) }

    private fun List<ProjectEntity>.filterByOwner(owner: String?) =
        if (owner.isNullOrBlank()) this else filter {
            if (owner == "Вы" || owner == "You") it.ownerName == "Вы" else it.ownerName == owner
        }

    private fun List<ProjectEntity>.filterByBudget(min: Double?, max: Double?) =
        filter {
            (min == null || it.budgetLimit >= min) &&
                    (max == null || it.budgetLimit <= max)
        }

    private fun List<ProjectEntity>.filterByStatuses(statuses: Set<ProjectStatus>): List<ProjectEntity> {
        // если хотят «ALL» или вообще не указано ни одного статуса — не режем список
        if (statuses.contains(ProjectStatus.ALL) || statuses.isEmpty()) {
            return this
        }
        return filter { it.status in statuses }
    }
}