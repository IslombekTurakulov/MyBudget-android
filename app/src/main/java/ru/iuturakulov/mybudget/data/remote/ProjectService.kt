package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class ProjectDto(
    val id: Int,
    val name: String,
    val description: String,
    val budgetLimit: Double,
    val amountSpent: Double,
    val status: String,
    val createdDate: String,
    val lastModified: String
)

data class TransactionDto(
    val id: Int,
    val projectId: Int,
    val userId: String,
    val name: String,
    val category: String,
    val categoryIcon: String,
    val amount: Double,
    val date: Long
)

/**
 * Работа с сервером
 */
interface ProjectService {

    // Получение списка проектов
    @GET("projects")
    suspend fun fetchProjects(): List<ProjectDto>

    // Получение деталей проекта
    @GET("projects/{id}")
    suspend fun fetchProjectDetails(@Path("id") projectId: Int): ProjectDto

    // Получение транзакций проекта
    @GET("projects/{id}/transactions")
    suspend fun fetchTransactions(@Path("id") projectId: Int): List<TransactionDto>

    // Добавление проекта
    @POST("projects")
    suspend fun addProject(@Body project: ProjectDto)

    // Обновление проекта
    @PUT("projects/{id}")
    suspend fun updateProject(@Path("id") projectId: Int, @Body project: ProjectDto): ProjectDto

    // Удаление проекта
    @DELETE("projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: Int)

    // Добавление транзакции в проект
    @POST("projects/{id}/transactions")
    suspend fun addTransaction(
        @Path("id") projectId: Int,
        @Body transaction: TransactionDto
    ): TransactionDto

    // Добавление транзакции в проект
    @POST("projects/{id}/transactions")
    suspend fun updateTransaction(
        @Path("id") projectId: Int,
        @Body transaction: TransactionDto
    ): TransactionDto

    // Удаление транзакции из проекта
    @DELETE("projects/{id}/transactions/{transactionId}")
    suspend fun deleteTransaction(
        @Path("id") projectId: Int,
        @Path("transactionId") transactionId: Int
    )
}
