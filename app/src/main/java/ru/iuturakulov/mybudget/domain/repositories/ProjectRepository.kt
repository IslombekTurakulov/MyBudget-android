package ru.iuturakulov.mybudget.domain.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.daos.TransactionDao
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectWithTransactions
import ru.iuturakulov.mybudget.data.mappers.TransactionMapper
import ru.iuturakulov.mybudget.data.remote.ProjectService
import ru.iuturakulov.mybudget.domain.mappers.ProjectMapper
import javax.inject.Inject

class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectService: ProjectService,
    private val transactionDao: TransactionDao
) {

    // Получение всех проектов в режиме Flow
    fun getProjectsFlow(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjectsFlow()
    }

    // Синхронизация данных с сервером
    suspend fun syncProjects(): Flow<List<ProjectEntity>> = flow {
        try {
            val response = projectService.fetchProjects()
            if (response.isSuccessful) {
                val remoteProjects =
                    response.body()?.map { ProjectMapper.dtoToEntity(it) } ?: emptyList()
                projectDao.insertProjects(remoteProjects) // Обновляем локальную базу
                emit(remoteProjects) // Отправляем результат в поток
            } else {
                throw Exception("Ошибка загрузки проектов: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("Ошибка синхронизации данных: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO) // Перенос выполнения в IO-поток

    suspend fun getCurrentRole(projectId: String): String? {
        return projectService.getCurrentRole(projectId).body()?.role
    }

    // Добавление нового проекта
    suspend fun addProject(project: ProjectEntity) {
        try {
            val response =
                projectService.addProject(ProjectMapper.entityToDto(project)) // Сохранение на сервере
            response.body()?.let { data ->
                projectDao.insertProject(ProjectMapper.dtoToEntity(data))
            } ?: throw Exception()
        } catch (e: Exception) {
            throw Exception("Ошибка добавления проекта: ${e.localizedMessage}")
        }
    }

    // Удаление проекта
    suspend fun deleteProject(projectId: String) {
        try {
            val response = projectService.deleteProject(projectId) // Удаление на сервере
            if (response.isSuccessful) {
                projectDao.deleteProject(projectId) // Локальное удаление
            }
        } catch (e: Exception) {
            throw Exception("Ошибка удаления проекта: ${e.localizedMessage}")
        }
    }

    // Обновление существующего проекта
    suspend fun updateProject(project: ProjectEntity) {
        try {
            val updatedDto = projectService.updateProject(
                project.id,
                ProjectMapper.entityToDto(project)
            ) // Обновление на сервере
            val body = updatedDto.body()
            requireNotNull(body)
            val updatedEntity = ProjectMapper.dtoToEntity(body)
            projectDao.updateProject(updatedEntity) // Синхронизируем локально
        } catch (e: Exception) {
            throw Exception("Ошибка обновления проекта: ${e.localizedMessage}")
        }
    }

    // Загрузка проекта с транзакциями
    suspend fun getProjectWithTransactions(projectId: String): ProjectWithTransactions {
        val projectDto = projectService.fetchProjectDetails(projectId).body()
        val transactionDtos = projectService.fetchTransactions(projectId).body()

        requireNotNull(projectDto)
        requireNotNull(transactionDtos)

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

    suspend fun getProjectByInviteCode(code: String): Boolean {
        val response = projectService.getProjectByInviteCode(code)
        if (!response.isSuccessful) {
            throw Exception(
                response.errorBody()?.string()
            )
        }
        return true
    }

    suspend fun addProjectLocally(project: ProjectEntity) {
        projectDao.insertProject(project)
    }
}
