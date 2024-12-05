package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.local.daos.ProjectDao
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.remote.ProjectDto
import ru.iuturakulov.mybudget.data.remote.ProjectService
import javax.inject.Inject

class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectService: ProjectService
) {

    // Получение списка проектов (локально -> удаленно при необходимости)
    suspend fun getProjects(): List<ProjectEntity> {
        val localProjects = projectDao.getAllProjects()
        return localProjects.ifEmpty {
            // Если локальных данных нет, загружаем с сервера
            val remoteProjects = projectService.fetchProjects()
            val entities = remoteProjects.map { dtoToEntity(it) }
            projectDao.insertProjects(entities)
            entities
        }
    }

    // Добавление нового проекта
    suspend fun addProject(project: ProjectEntity) {
        // Сначала сохраняем проект локально
        projectDao.insertProject(project)
        // Затем отправляем его на сервер
        projectService.addProject(entityToDto(project))
    }

    // Удаление проекта
    suspend fun deleteProject(projectId: Int) {
        // Удаляем локально
        projectDao.deleteProject(projectId)
        // Удаляем на сервере
        projectService.deleteProject(projectId)
    }

    // Синхронизация данных с сервером (например, при запуске приложения)
    suspend fun syncProjects() {
        val remoteProjects = projectService.fetchProjects()
        val entities = remoteProjects.map { dtoToEntity(it) }
        projectDao.insertProjects(entities)
    }

    // Маппинг DTO -> Entity
    private fun dtoToEntity(dto: ProjectDto): ProjectEntity {
        return ProjectEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            budgetLimit = dto.budgetLimit
        )
    }

    // Маппинг Entity -> DTO
    private fun entityToDto(entity: ProjectEntity): ProjectDto {
        return ProjectDto(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            budgetLimit = entity.budgetLimit
        )
    }
}