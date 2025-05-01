package ru.iuturakulov.mybudget.data.remote.dto

import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus

data class ProjectDto(
    val id: String,
    val name: String,
    val description: String?,
    val budgetLimit: Double,
    val amountSpent: Double,
    val status: ProjectStatus,
    val createdAt: Long,
    val lastModified: Long,
    val category: String?,
    val categoryIcon: String?,
    val ownerId: String,
    val ownerName: String,
    val ownerEmail: String
)
