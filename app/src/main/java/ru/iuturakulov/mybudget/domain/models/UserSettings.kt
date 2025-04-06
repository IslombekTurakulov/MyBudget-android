package ru.iuturakulov.mybudget.domain.models

data class UserSettings(
    val name: String,
    val email: String,
    val language: String,
    val notificationsEnabled: Boolean,
    val darkThemeEnabled: Boolean
)