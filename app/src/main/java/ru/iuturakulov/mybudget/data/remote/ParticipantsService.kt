package ru.iuturakulov.mybudget.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.InvitationRequest
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantDto

interface ParticipantsService {

    @GET("projects/{projectId}/participants")
    suspend fun getParticipantsForProject(@Path("projectId") projectId: String): Response<List<ParticipantDto>>

    @POST("projects/participants")
    suspend fun addOrUpdateParticipant(@Body participant: ParticipantDto): Response<Unit>

    @DELETE("projects/{projectId}/participants/{participantId}")
    suspend fun deleteParticipant(@Path("projectId") projectId: String, @Path("participantId") participantId: String): Response<Unit>

    @POST("projects/{projectId}/invite")
    suspend fun sendInvitation(@Path("projectId") projectId: String, @Body invitationRequest: InvitationRequest): Response<Unit>
}