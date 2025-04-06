package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.local.daos.UserSettingsDao
import ru.iuturakulov.mybudget.data.local.entities.UserSettingsEntity
import ru.iuturakulov.mybudget.data.remote.SettingsService
import ru.iuturakulov.mybudget.data.remote.dto.UserSettingsDto
import ru.iuturakulov.mybudget.domain.models.UserSettings
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val settingsService: SettingsService,
    private val userSettingsDao: UserSettingsDao
) {

    suspend fun getUserSettings(): UserSettings {
        val localSettings = userSettingsDao.getUserSettings("current_user_email")
        return localSettings?.toDomain() ?: fetchAndCacheSettings()
    }

    suspend fun updateUserSettings(settings: UserSettings): UserSettings {
        val updatedDto = settingsService.updateUserSettings(settings)
        userSettingsDao.insertUserSettings(updatedDto.toEntity())
        return updatedDto
    }

    private suspend fun fetchAndCacheSettings(): UserSettings {
        val dto = settingsService.getUserSettings()
        userSettingsDao.insertUserSettings(dto.toEntity())
        return dto
    }

    private fun UserSettingsDto.toDomain() = UserSettings(
        name = name,
        email = email,
        language = language,
        notificationsEnabled = notificationsEnabled,
        darkThemeEnabled = darkThemeEnabled
    )

    private fun UserSettings.toDto() = UserSettingsDto(
        name = name,
        email = email,
        language = language,
        notificationsEnabled = notificationsEnabled,
        darkThemeEnabled = darkThemeEnabled,
    )

    private fun UserSettingsEntity.toDomain() = UserSettings(
        name = name,
        email = email,
        language = language,
        notificationsEnabled = notificationsEnabled,
        darkThemeEnabled = darkThemeEnabled,
    )

    private fun UserSettings.toEntity() = UserSettingsEntity(
        email = email,
        name = name,
        language = language,
        notificationsEnabled = notificationsEnabled,
        darkThemeEnabled = darkThemeEnabled,
    )
}
