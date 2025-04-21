package ru.iuturakulov.mybudget.data.remote.dto

import android.content.Context
import androidx.annotation.StringRes
import ru.iuturakulov.mybudget.R

data class ParticipantDto(
    val id: String,         // ID участника на сервере
    val projectId: String,
    val userId: String,  // Уникальный идентификатор пользователя
    val name: String,    // Имя участника
    val email: String,   // Email участника
    val role: ParticipantRole     // Роль участника
)


enum class ParticipantRole(@StringRes val displayNameRes: Int) {
    VIEWER(R.string.role_viewer),
    EDITOR(R.string.role_editor),
    OWNER(R.string.role_admin);

    companion object {
        /**
         * Находит роль по локализованному имени
         * @param context Контекст для доступа к ресурсам
         * @param displayName Локализованное название роли
         * @return Найденная роль или null если не найдена
         */
        fun fromDisplayName(context: Context, displayName: String): ParticipantRole? {
            return values().firstOrNull { role ->
                context.getString(role.displayNameRes) == displayName
            }
        }
    }

    fun getDisplayName(context: Context): String {
        return context.getString(displayNameRes)
    }
}
