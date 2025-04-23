package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole

data class ProjectWithTransactions(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val transactions: List<TransactionEntity>,
    val currentRole: ParticipantRole? = ParticipantRole.VIEWER
)
