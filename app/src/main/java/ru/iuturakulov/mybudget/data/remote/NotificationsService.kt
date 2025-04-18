package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.NotificationDto

interface NotificationsService {
    @GET("notifications")
    suspend fun getAll(): List<NotificationDto>

    @POST("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)
}
