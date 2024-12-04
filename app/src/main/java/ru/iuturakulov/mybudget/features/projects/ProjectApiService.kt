package ru.iuturakulov.mybudget.features.projects

import retrofit2.http.GET

interface ProjectApiService {
    @GET("projects")
    suspend fun getProjects(): List<Project>

    @GET("categories")
    suspend fun getCategories(): List<Category>
}