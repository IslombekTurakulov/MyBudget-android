package ru.iuturakulov.mybudget.data.local.entities

import android.content.Context
import androidx.core.content.ContextCompat
import ru.iuturakulov.mybudget.R

enum class ProjectStatus(val type: String) {
    ACTIVE("active"),
    DELETED("completed"),
    ARCHIVED("cancelled"),
    ALL("all");

    fun canTransitionTo(newStatus: ProjectStatus): Boolean {
        val allowedTransitions = mapOf(
            ACTIVE to listOf(DELETED, ARCHIVED),
            ALL to listOf(ACTIVE, DELETED, ARCHIVED),
            DELETED to emptyList(),
            ARCHIVED to emptyList()
        )
        return allowedTransitions[this]?.contains(newStatus) == true
    }

    companion object {
        fun ProjectStatus.getStatusColor(context: Context): Int {
            return when (this) {
                ACTIVE -> ContextCompat.getColor(context, R.color.green)
                DELETED -> ContextCompat.getColor(context, R.color.blue)
                ARCHIVED -> ContextCompat.getColor(context, R.color.red)
                ALL -> ContextCompat.getColor(context, R.color.gray)
            }
        }

        fun ProjectStatus.getStatusText(): String {
            return when (this) {
                ACTIVE -> "Активный"
                DELETED -> "Удален"
                ARCHIVED -> "Архивирован"
                ALL -> ""
            }
        }
    }
}
