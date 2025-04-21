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
    private val participantService: ParticipantsService,
) {
    companion object {
        private const val SYNC_THRESHOLD_MINUTES = 15L
    }

    /**
     * Получение участников только из локальной базы
     */
    fun observeLocalParticipants(projectId: String): Flow<List<ParticipantEntity>> =
        participantDao.getParticipantsForProject(projectId)

    /**
     * Получение участников с автоматической синхронизацией (если нужно)
     */
    suspend fun getParticipantsWithSync(projectId: String): Flow<List<ParticipantEntity>> {
        syncParticipants(projectId)
        return observeLocalParticipants(projectId)
    }

    /**
     * Синхронизация участников с сервером
     * @return Синхронизированный список участников
     * @throws ParticipantsSyncException при ошибках синхронизации
     */
    suspend fun syncParticipants(projectId: String): List<ParticipantEntity> {
        return try {
            val response = participantService.getParticipantsForProject(projectId)

            if (!response.isSuccessful) {
                throw ParticipantsSyncException("Ошибка сервера: ${response.code()}")
            }

            val remoteParticipants = response.body().orEmpty()
            val entities = remoteParticipants.map { ParticipantMapper.dtoToEntity(it) }

            participantDao.replaceParticipantsForProject(projectId, entities)
            entities
        } catch (e: Exception) {
            throw ParticipantsSyncException(
                "Ошибка синхронизации: ${e.localizedMessage}",
                e
            )
        }
    }
    /**
     * Сохранение участника с синхронизацией
     * @throws ParticipantOperationException при ошибках
     */
    suspend fun saveParticipant(participant: ParticipantEntity) {
        try {

            val response = participantService.addOrUpdateParticipant(
                projectId = participant.projectId,
                participant = ParticipantMapper.entityToDto(participant)
            )

            if (!response.isSuccessful) {
                throw ParticipantOperationException("Ошибка сервера: ${response.code()}")
            }

            participantDao.insertParticipant(participant)
        } catch (e: Exception) {
            throw ParticipantOperationException(
                "Ошибка сохранения участника: ${e.localizedMessage}",
                e
            )
        }
    }

    /**
     * Удаление участника
     * @throws ParticipantOperationException при ошибках
     */
    suspend fun deleteParticipant(projectId: String, participantId: String) {
        try {

            val response = participantService.deleteParticipant(projectId, participantId)

            if (!response.isSuccessful) {
                throw ParticipantOperationException("Ошибка сервера: ${response.code()}")
            }

            participantDao.deleteParticipant(projectId, participantId)
        } catch (e: Exception) {
            throw ParticipantOperationException(
                "Ошибка удаления участника: ${e.localizedMessage}",
                e
            )
        }
    }

    /**
     * Отправка приглашения
     * @throws InvitationException при ошибках
     */
    suspend fun sendInvitation(projectId: String, email: String, role: String) {
        try {
            val response = participantService.sendInvitation(
                projectId,
                InvitationRequest(projectId, email, role)
            )

            if (!response.isSuccessful) {
                throw InvitationException("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            throw InvitationException(
                "${e.localizedMessage}",
                e
            )
        }
    }

    class ParticipantsSyncException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    class ParticipantOperationException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    class InvitationException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)
}

