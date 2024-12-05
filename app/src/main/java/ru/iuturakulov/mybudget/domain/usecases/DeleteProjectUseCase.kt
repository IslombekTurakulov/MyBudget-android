package ru.iuturakulov.mybudget.domain.usecases

import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {

    suspend operator fun invoke(projectId: Int) {
        repository.deleteProject(projectId)
    }
}