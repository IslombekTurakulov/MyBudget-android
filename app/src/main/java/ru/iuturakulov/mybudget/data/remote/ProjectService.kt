package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.ProjectDto
import ru.iuturakulov.mybudget.data.remote.dto.TransactionDto

/**
 * Работа с сервером
 */
interface ProjectService {

    // Получение списка проектов
    @GET("projects")
    suspend fun fetchProjects(): List<ProjectDto>

    // Получение деталей проекта
    @GET("projects/{id}")
    suspend fun fetchProjectDetails(@Path("id") projectId: String): ProjectDto

    // Получение транзакций проекта
    @GET("projects/{id}/transactions")
    suspend fun fetchTransactions(@Path("id") projectId: String): List<TransactionDto>

    // Добавление проекта
    @POST("projects")
    suspend fun addProject(@Body project: ProjectDto)

    // Обновление проекта
    @PUT("projects/{id}")
    suspend fun updateProject(@Path("id") projectId: String, @Body project: ProjectDto): ProjectDto

    @GET("projects/invite/{code}")
    suspend fun getProjectByInviteCode(@Path("code") code: String): ProjectDto

    // Удаление проекта
    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: String)

    // Добавление транзакции в проект
    @POST("projects/{id}/transactions")
    suspend fun addTransaction(
        @Path("id") projectId: String,
        @Body transaction: TransactionDto
    ): TransactionDto

    // Добавление транзакции в проект
    @POST("projects/{id}/transactions")
    suspend fun updateTransaction(
        @Path("id") projectId: String,
        @Body transaction: TransactionDto
    ): TransactionDto

    // Удаление транзакции из проекта
    @DELETE("projects/{id}/transactions/{transactionId}")
    suspend fun deleteTransaction(
        @Path("id") projectId: String,
        @Path("transactionId") transactionId: String
    )
}
