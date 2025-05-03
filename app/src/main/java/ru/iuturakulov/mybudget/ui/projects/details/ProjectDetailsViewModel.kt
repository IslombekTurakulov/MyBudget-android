package ru.iuturakulov.mybudget.ui.projects.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectWithTransactions
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.domain.models.TransactionFilter
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import ru.iuturakulov.mybudget.domain.repositories.TransactionRepository
import javax.inject.Inject

@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ProjectWithTransactions>>(UiState.Idle)
    val uiState: StateFlow<UiState<ProjectWithTransactions>> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateState: StateFlow<UiState<Unit>> = _updateState.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _currentFilter = MutableStateFlow(TransactionFilter())
    val currentFilter: StateFlow<TransactionFilter> = _currentFilter

    val filteredTransactions = combine(_transactions, _currentFilter) { list, filter ->
        applyFilterInternal(list, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), emptyList())

    /**
     * Загрузка проекта с транзакциями.
     */
    fun loadProjectDetails(projectId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val projectDetails = projectRepository.getProjectWithTransactions(projectId)
                _uiState.value = UiState.Success(projectDetails)
                _transactions.value = projectDetails.transactions
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Ошибка загрузки данных: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Добавление транзакции.
     */
    fun addTransaction(projectId: String, transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                transactionRepository.addTransaction(projectId, transaction)
                syncTransactions(projectId)
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error("${e.localizedMessage}")
            }
        }
    }

    /**
     * Обновление транзакции.
     */
    fun updateTransaction(projectId: String, transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                transactionRepository.updateTransaction(projectId, transaction)
                syncTransactions(projectId)
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error("${e.localizedMessage}")
            }
        }
    }

    /**
     * Удаление транзакции.
     */
    fun deleteTransaction(projectId: String, transactionId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(projectId, transactionId)
                syncTransactions(projectId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Ошибка удаления транзакции: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Синхронизация транзакций с сервером.
     */
    fun syncTransactions(projectId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.syncTransactions(projectId)
                val updatedProject = projectRepository.getProjectWithTransactions(projectId)
                _uiState.value = UiState.Success(updatedProject)
                _transactions.value = updatedProject.transactions
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error("Ошибка синхронизации транзакций: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Применение фильтра.
     */
    fun applyFilter(filter: TransactionFilter) {
        _currentFilter.value = filter
    }

    private fun applyFilterInternal(
        transactions: List<TransactionEntity>,
        filter: TransactionFilter
    ): List<TransactionEntity> {
        return transactions.filter { t ->
            val signedAmount = when (TransactionEntity.TransactionType.fromString(t.type)) {
                TransactionEntity.TransactionType.INCOME  ->  t.amount
                TransactionEntity.TransactionType.EXPENSE -> -t.amount
            }

            // категория
            (filter.category == null || t.category == filter.category) &&
                    // пользователь
                    (filter.userName == null || t.userName == filter.userName) &&
                    // тип
                    (filter.type == null || t.type == filter.type.typeName) &&
                    // диапазон дат
                    (filter.startDate?.let { t.date >= it } ?: true) &&
                    (filter.endDate?.let { t.date <= it } ?: true) &&
                    // диапазон суммы
                    (filter.minAmount == null || signedAmount >= filter.minAmount) &&
                    (filter.maxAmount == null || signedAmount <= filter.maxAmount)
        }
    }

    /**
     * Синхронизация данных проекта.
     */
    fun syncProjectDetails(projectId: String) {
        viewModelScope.launch {
            try {
                projectRepository.syncProjects()
                loadProjectDetails(projectId)
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error("Ошибка синхронизации проекта: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Синхронизация всех проектов
     */
    fun syncProjects() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                projectRepository.syncProjects()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка загрузки")
            }
        }
    }

    /**
     * Удаление проекта.
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                projectRepository.deleteProject(projectId)
                _uiState.value = UiState.Success(null) // Возврат на предыдущий экран
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Ошибка удаления проекта: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Обновление проекта.
     */
    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            try {
                projectRepository.updateProject(project)
                loadProjectDetails(project.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("${e.localizedMessage}")
            }
        }
    }

    fun editProject(project: ProjectEntity) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            try {
                projectRepository.updateProject(project)
                loadProjectDetails(project.id)
                _updateState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _updateState.value =
                    UiState.Error(e.localizedMessage ?: "Ошибка обновления проекта")
            } finally {
                _updateState.value = UiState.Idle
            }
        }
    }
}
