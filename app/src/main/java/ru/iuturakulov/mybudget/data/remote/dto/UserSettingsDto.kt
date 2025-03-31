package ru.iuturakulov.mybudget.data.remote.dto

@kotlinx.serialization.Serializable
data class UserSettingsDto(
    val name: String,
    val email: String,
    val language: String,
    val notificationsEnabled: Boolean
)