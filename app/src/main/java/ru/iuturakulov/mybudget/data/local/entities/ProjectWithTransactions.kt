package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ProjectWithTransactions(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val transactions: List<TransactionEntity>
)
