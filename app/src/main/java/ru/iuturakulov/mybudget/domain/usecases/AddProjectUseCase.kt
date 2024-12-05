package ru.iuturakulov.mybudget.domain.usecases

import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

class AddProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {

    suspend operator fun invoke(project: ProjectEntity) {
        repository.addProject(project)
    }
}