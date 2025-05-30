package ru.iuturakulov.mybudget.domain.usecases.project

import kotlinx.coroutines.flow.Flow
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

class FetchProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {

    fun execute(): Flow<List<ProjectEntity>> {
        return repository.getProjectsFlow()
    }
}