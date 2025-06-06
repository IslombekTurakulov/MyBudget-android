package ru.iuturakulov.mybudget.domain.mappers

import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.remote.dto.ProjectDto

object ProjectMapper {

    fun dtoToEntity(dto: ProjectDto): ProjectEntity {
        return ProjectEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            budgetLimit = dto.budgetLimit,
            amountSpent = dto.amountSpent,
            status = dto.status,
            createdAt = dto.createdAt,
            lastModified = dto.lastModified,
            categoryIcon = dto.categoryIcon,
            category = dto.category,
            ownerName = dto.ownerName,
            ownerEmail = dto.ownerEmail,
            ownerId = dto.ownerId
        )
    }

    fun entityToDto(entity: ProjectEntity): ProjectDto {
        return ProjectDto(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            budgetLimit = entity.budgetLimit,
            amountSpent = entity.amountSpent,
            status = entity.status,
            createdAt = entity.createdAt,
            lastModified = entity.lastModified,
            categoryIcon = entity.categoryIcon,
            category = entity.category,
            ownerName = entity.ownerName,
            ownerEmail = entity.ownerEmail,
            ownerId = entity.ownerId
        )
    }
}
