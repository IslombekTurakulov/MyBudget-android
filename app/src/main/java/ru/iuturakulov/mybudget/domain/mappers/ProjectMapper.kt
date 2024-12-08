package ru.iuturakulov.mybudget.domain.mappers

import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.remote.ProjectDto

object ProjectMapper {

    fun dtoToEntity(dto: ProjectDto): ProjectEntity {
        return ProjectEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            budgetLimit = dto.budgetLimit,
            amountSpent = dto.amountSpent,
            status = dto.status,
            createdDate = dto.createdDate,
            lastModified = dto.lastModified
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
            createdDate = entity.createdDate,
            lastModified = entity.lastModified
        )
    }
}
