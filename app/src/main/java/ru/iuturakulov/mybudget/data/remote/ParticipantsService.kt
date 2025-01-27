package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.InvitationRequest
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantDto

interface ParticipantsService {

    @GET("projects/{projectId}/participants")
    suspend fun getParticipantsForProject(@Path("projectId") projectId: String): List<ParticipantDto>

    @POST("projects/participants")
    suspend fun addOrUpdateParticipant(@Body participant: ParticipantDto)

    @DELETE("projects/participants/{id}")
    suspend fun deleteParticipant(@Path("id") participantId: Int)

    @POST("projects/invitations")
    suspend fun sendInvitation(@Body invitationRequest: InvitationRequest)
}