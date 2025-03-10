package ru.iuturakulov.mybudget.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity

@Dao
interface ParticipantsDao {

    @Query("SELECT * FROM participants WHERE projectId = :projectId")
    fun getParticipantsForProject(projectId: String): Flow<List<ParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM participants WHERE id = :participantId")
    suspend fun deleteParticipant(participantId: String)

    @Transaction
    suspend fun replaceParticipantsForProject(
        projectId: String,
        participants: List<ParticipantEntity>
    ) {
        deleteParticipantsByProject(projectId)
        insertParticipants(participants)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Query("DELETE FROM participants WHERE projectId = :projectId")
    suspend fun deleteParticipantsByProject(projectId: String)
}
