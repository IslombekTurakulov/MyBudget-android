package ru.iuturakulov.mybudget.domain.usecases.project

import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

class SyncProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {

    suspend fun execute() {
        // return repository.syncProjects()
    }
}