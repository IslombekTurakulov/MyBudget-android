package ru.iuturakulov.mybudget.domain.usecases

import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

class FetchProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {

    suspend operator fun invoke(): List<ProjectEntity> {
        return repository.getProjects()
    }
}