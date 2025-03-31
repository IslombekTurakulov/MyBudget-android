package ru.iuturakulov.mybudget.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import ru.iuturakulov.mybudget.domain.models.UserSettings

interface SettingsService {

    @GET("settings")
    suspend fun getUserSettings(): UserSettings

    @PUT("settings")
    suspend fun updateUserSettings(@Body settings: UserSettings): UserSettings
}
