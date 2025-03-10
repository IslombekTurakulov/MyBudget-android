package ru.iuturakulov.mybudget.domain.repositories

import kotlinx.coroutines.flow.Flow
import ru.iuturakulov.mybudget.data.local.daos.ParticipantsDao
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.mappers.ParticipantMapper
import ru.iuturakulov.mybudget.data.remote.ParticipantsService
import ru.iuturakulov.mybudget.data.remote.dto.InvitationRequest
import javax.inject.Inject

class ParticipantsRepository @Inject constructor(
    private val participantDao: ParticipantsDao,
    private val participantService: ParticipantsService
) {

    /**
     * Получение участников проекта из локальной базы данных.
     */
    suspend fun getParticipantsForProject(projectId: String): Flow<List<ParticipantEntity>> {
        syncParticipants(projectId).also {
            return participantDao.getParticipantsForProject(projectId)
        }
    }

    /**
     * Сохранение участника (локально и на сервере).
     */
    suspend fun saveParticipant(participant: ParticipantEntity) {
        try {
            val response =
                participantService.addOrUpdateParticipant(ParticipantMapper.entityToDto(participant))
            if (response.isSuccessful) {
                participantDao.insertParticipant(participant)
            }
        } catch (e: Exception) {
            throw Exception("Ошибка сохранения участника на сервере: ${e.localizedMessage}")
        }
    }

    /**
     * Удаление участника (локально и на сервере).
     */
    suspend fun deleteParticipant(participantId: String) {
        try {
            val response = participantService.deleteParticipant(participantId)
            if (response.isSuccessful) {
                participantDao.deleteParticipant(participantId)
            }
        } catch (e: Exception) {
            throw Exception("Ошибка удаления участника на сервере: ${e.localizedMessage}")
        }
    }

    /**
     * Синхронизация участников с сервером.
     */
    suspend fun syncParticipants(projectId: String): List<ParticipantEntity> {
        try {
            val remoteParticipants = participantService.getParticipantsForProject(projectId).body()
            val entities = remoteParticipants?.map { ParticipantMapper.dtoToEntity(it) }.orEmpty()
            participantDao.replaceParticipantsForProject(projectId, entities)
            return entities
        } catch (e: Exception) {
            throw Exception("Ошибка синхронизации участников: ${e.localizedMessage}")
        }
    }

    /**
     * Отправка приглашения на сервер.
     */
    suspend fun sendInvitation(projectId: String, email: String, role: String) {
        try {
            participantService.sendInvitation(InvitationRequest(projectId, email, role))
        } catch (e: Exception) {
            throw Exception("Ошибка отправки приглашения: ${e.localizedMessage}")
        }
    }
}



