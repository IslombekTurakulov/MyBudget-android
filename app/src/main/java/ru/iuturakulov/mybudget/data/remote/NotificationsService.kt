package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.iuturakulov.mybudget.data.remote.dto.NotificationDto

interface NotificationsService {
    @GET("notifications")
    suspend fun getAllNotifications(): List<NotificationDto>

    @PUT("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)

    @DELETE("notifications/{id}")
    suspend fun removeNotification(@Path("id") id: String)
}
