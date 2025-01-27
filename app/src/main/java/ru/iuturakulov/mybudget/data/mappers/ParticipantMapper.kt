package ru.iuturakulov.mybudget.data.mappers

import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantDto

object ParticipantMapper {

    fun dtoToEntity(dto: ParticipantDto): ParticipantEntity {
        return ParticipantEntity(
            id = dto.id,
            projectId = dto.projectId,
            name = dto.name,
            role = dto.role,
            email = dto.email,
            userId = dto.userId
        )
    }

    fun entityToDto(entity: ParticipantEntity): ParticipantDto {
        return ParticipantDto(
            id = entity.id,
            projectId = entity.projectId,
            name = entity.name,
            role = entity.role,
            email = entity.email,
            userId = entity.userId
        )
    }
}
