package ru.iuturakulov.mybudget.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.InvitationPreferencesResponse
import ru.iuturakulov.mybudget.data.remote.dto.PreferencesRequest
import ru.iuturakulov.mybudget.data.remote.dto.RegisterDeviceRequest

interface FCMService {
    @POST("devices/register")
    suspend fun registerDevice(@Body dto: RegisterDeviceRequest): Response<Unit>

    @GET("projects/{id}/notifications/preferences")
    suspend fun getProjectNotificationPreferences(@Path("id") projectId: String): Response<InvitationPreferencesResponse>

    @POST("projects/{id}/notifications/preferences")
    suspend fun updateProjectNotificationPreferences(
        @Path("id") projectId: String,
        @Body req: PreferencesRequest
    ): Response<Unit>
}