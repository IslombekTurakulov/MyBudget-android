package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class ProjectDto(
    val id: Int,
    val name: String,
    val description: String,
    val budgetLimit: Double
)

/**
 * Работа с сервером
 */
interface ProjectService {

    @GET("projects")
    suspend fun fetchProjects(): List<ProjectDto>

    @POST("projects")
    suspend fun addProject(@Body project: ProjectDto)

    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: Int)
}