package ru.iuturakulov.mybudget.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectWithTransactions
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.mappers.TransactionMapper
import ru.iuturakulov.mybudget.data.remote.ProjectDto
import ru.iuturakulov.mybudget.data.remote.ProjectService
import ru.iuturakulov.mybudget.domain.mappers.ProjectMapper
import ru.iuturakulov.mybudget.domain.models.TransactionFilter
import javax.inject.Inject

class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectService: ProjectService,
    private val transactionDao: TransactionDao
) {

    // Получение всех проектов в режиме Flow
    fun getProjectsFlow(): Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    // Синхронизация данных с сервером
    suspend fun syncProjects() {
        try {
            val remoteProjects = projectService.fetchProjects()
            val entities = remoteProjects.map { ProjectMapper.dtoToEntity(it) }
            projectDao.insertProjects(entities) // Обновляем локальную базу
        } catch (e: Exception) {
            throw Exception("Ошибка синхронизации данных: ${e.localizedMessage}")
        }
    }

    // Добавление нового проекта
    suspend fun addProject(project: ProjectEntity) {
        projectDao.insertProject(project) // Локальное добавление
        try {
            projectService.addProject(ProjectMapper.entityToDto(project)) // Сохранение на сервере
        } catch (e: Exception) {
            projectDao.deleteProject(project.id) // Удаляем локально при ошибке
            throw Exception("Ошибка добавления проекта: ${e.localizedMessage}")
        }
    }

    // Удаление проекта
    suspend fun deleteProject(projectId: Int) {
        projectDao.deleteProject(projectId) // Локальное удаление
        try {
            projectService.deleteProject(projectId) // Удаление на сервере
        } catch (e: Exception) {
            throw Exception("Ошибка удаления проекта: ${e.localizedMessage}")
        }
    }

    // Обновление существующего проекта
    suspend fun updateProject(project: ProjectEntity) {
        try {
            val updatedDto = projectService.updateProject(project.id, ProjectMapper.entityToDto(project)) // Обновление на сервере
            val updatedEntity = ProjectMapper.dtoToEntity(updatedDto)
            projectDao.updateProject(updatedEntity) // Синхронизируем локально
        } catch (e: Exception) {
            throw Exception("Ошибка обновления проекта: ${e.localizedMessage}")
        }
    }

    // Загрузка конкретного проекта
    suspend fun getProjectById(projectId: Int): ProjectEntity {
        return projectDao.getProjectById(projectId) ?: throw Exception("Проект не найден")
    }

    // Загрузка проекта с транзакциями
    suspend fun getProjectWithTransactions(projectId: Int): ProjectWithTransactions {
        val localProjectWithTransactions = projectDao.getProjectWithTransactions(projectId)
        if (localProjectWithTransactions != null) {
            return localProjectWithTransactions
        }

        // Если локальных данных нет, загружаем с сервера
        val projectDto = projectService.fetchProjectDetails(projectId)
        val transactionDtos = projectService.fetchTransactions(projectId)

        val projectEntity = ProjectMapper.dtoToEntity(projectDto)
        val transactionEntities = transactionDtos.map { TransactionMapper.dtoToEntity(it) }

        projectDao.insertProject(projectEntity)
        transactionDao.insertTransactions(transactionEntities)

        return ProjectWithTransactions(projectEntity, transactionEntities)
    }

//    // Фильтрация транзакций для проекта
//    suspend fun filterTransactions(
//        projectId: Int,
//        filter: TransactionFilter
//    ): List<TransactionEntity> {
//        val transactions = transactionDao.getTransactionsForProject(projectId)
//        return transactions.filter { transaction ->
//            (filter.category == null || transaction.category == filter.category) &&
//                    (filter.minAmount == null || transaction.amount >= filter.minAmount) &&
//                    (filter.maxAmount == null || transaction.amount <= filter.maxAmount)
//        }
//    }
}
