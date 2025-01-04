package ru.iuturakulov.mybudget.ui.projects.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow<UiState<ProjectWithTransactions>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProjectWithTransactions>> = _uiState.asStateFlow()

    private val _filteredTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val filteredTransactions: StateFlow<List<TransactionEntity>> =
        _filteredTransactions.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val currentFilter = MutableStateFlow(TransactionFilter())

    init {
        observeFilterChanges()
    }

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
                applyCurrentFilter(projectDetails.transactions)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Ошибка загрузки данных: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Наблюдение за изменением фильтра и динамическая фильтрация.
     */
    private fun observeFilterChanges() {
        viewModelScope.launch {
            currentFilter.collect { filter ->
                val transactions = _transactions.value
                applyCurrentFilter(transactions)
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
                    UiState.Error("Ошибка добавления транзакции: ${e.localizedMessage}")
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
                    UiState.Error("Ошибка добавления транзакции: ${e.localizedMessage}")
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
        currentFilter.value = filter
    }

    private fun applyCurrentFilter(transactions: List<TransactionEntity>) {
        val filter = currentFilter.value
        val minAmount = filter.minAmount
        val maxAmount = filter.maxAmount
        val category = filter.category

        _filteredTransactions.value = transactions.filter { transaction ->
            (category == null || transaction.category == category) &&
                    (minAmount == null || transaction.amount >= minAmount) &&
                    (maxAmount == null || transaction.amount <= maxAmount)
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
                _uiState.value = UiState.Error("Ошибка обновления проекта: ${e.localizedMessage}")
            }
        }
    }
}
