package ru.iuturakulov.mybudget.data.mappers

import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.remote.dto.TransactionDto

object TransactionMapper {

    fun dtoToEntity(dto: TransactionDto): TransactionEntity {
        return TransactionEntity(
            id = dto.id,
            projectId = dto.projectId,
            name = dto.name,
            category = dto.category,
            categoryIcon = dto.categoryIcon, // Преобразуем иконку
            amount = dto.amount,
            date = dto.date,
            userId = dto.userId
        )
    }

    fun entityToDto(entity: TransactionEntity): TransactionDto {
        return TransactionDto(
            id = entity.id,
            projectId = entity.projectId,
            name = entity.name,
            category = entity.category,
            categoryIcon = entity.categoryIcon, // Преобразуем обратно
            amount = entity.amount,
            date = entity.date,
            userId = entity.userId
        )
    }
}
