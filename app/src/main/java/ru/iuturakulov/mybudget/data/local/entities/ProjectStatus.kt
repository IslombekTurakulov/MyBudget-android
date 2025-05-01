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
            ACTIVE to listOf(ACTIVE, DELETED, ARCHIVED),
            ALL to listOf(ACTIVE, DELETED, ARCHIVED),
            DELETED to emptyList(),
            ARCHIVED to emptyList()
        )
        return allowedTransitions[this]?.contains(newStatus) == true
    }

    fun getStatusText(): String = when (this) {
        ProjectStatus.ACTIVE -> "Активен"
        ProjectStatus.ARCHIVED -> "В архиве"
        ProjectStatus.DELETED -> "Удалён"
        ProjectStatus.ALL -> "Все"
    }

    fun getStatusColor(context: Context): Int = when (this) {
        ProjectStatus.ACTIVE -> ContextCompat.getColor(context, R.color.green)
        ProjectStatus.ARCHIVED -> ContextCompat.getColor(context, R.color.gray)
        ProjectStatus.DELETED -> ContextCompat.getColor(context, R.color.red)
        else -> ContextCompat.getColor(context, R.color.networkStatusBackgroundOff)
    }

    fun getStatusIcon(): Int = when (this) {
        ProjectStatus.ACTIVE -> R.drawable.ic_status_active
        ProjectStatus.ARCHIVED -> R.drawable.ic_status_archived
        ProjectStatus.DELETED -> R.drawable.ic_status_deleted
        else -> R.drawable.ic_status_all
    }
}
