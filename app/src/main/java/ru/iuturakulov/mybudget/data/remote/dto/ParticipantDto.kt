package ru.iuturakulov.mybudget.data.remote.dto

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus

data class ParticipantDto(
    val id: String,
    val projectId: String,
    val userId: String,
    val name: String,
    val email: String,
    val role: ParticipantRole
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

    fun getStatusColor(context: Context): Int = ContextCompat.getColor(
        context, when (this) {
            VIEWER -> R.color.role_viewer
            EDITOR -> R.color.role_editor
            OWNER -> R.color.role_owner
        }
    )
}
