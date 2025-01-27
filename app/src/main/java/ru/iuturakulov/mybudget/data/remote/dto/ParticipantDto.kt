package ru.iuturakulov.mybudget.data.remote.dto

data class ParticipantDto(
    val id: Int,         // ID участника на сервере
    val projectId: String,
    val userId: String,  // Уникальный идентификатор пользователя
    val name: String,    // Имя участника
    val email: String,   // Email участника
    val role: String     // Роль участника
)
