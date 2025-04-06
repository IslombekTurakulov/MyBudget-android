package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val email: String,
    val name: String,
    val language: String,
    val notificationsEnabled: Boolean,
    val darkThemeEnabled: Boolean
)