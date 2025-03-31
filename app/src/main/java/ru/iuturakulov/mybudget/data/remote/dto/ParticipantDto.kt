package ru.iuturakulov.mybudget.data.remote.dto

data class ParticipantDto(
    val id: String,         // ID участника на сервере
    val projectId: String,
    val userId: String,  // Уникальный идентификатор пользователя
    val name: String,    // Имя участника
    val email: String,   // Email участника
    val role: String     // Роль участника
)


enum class ParticipantRole {
    OWNER,      // Владелец проекта (может всё)
    EDITOR,     // Редактор (может изменять проект, добавлять/удалять транзакции)
    VIEWER;     // Наблюдатель (только просмотр)

    companion object {
        fun fromString(role: String): ParticipantRole? {
            return entries.find { it.name.equals(role, ignoreCase = true) }
        }
    }
}