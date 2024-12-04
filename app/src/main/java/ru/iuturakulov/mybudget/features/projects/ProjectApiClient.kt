package ru.iuturakulov.mybudget.features.projects

import ru.iuturakulov.mybudget.core.network.ApiResponse
import ru.iuturakulov.mybudget.core.network.BaseApiClient

class ProjectApiClient(apiService: ProjectApiService) : BaseApiClient<ProjectApiService>(apiService) {

    suspend fun getProjects(): ApiResponse<List<Project>> {
        return safeApiCall { getProjects() }
    }
}