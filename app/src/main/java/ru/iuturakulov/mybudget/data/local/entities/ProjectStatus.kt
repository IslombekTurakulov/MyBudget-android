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

    fun getStatusText(context: Context): String = when (this) {
        ACTIVE -> ContextCompat.getString(context, R.string.project_status_active)
        ARCHIVED -> ContextCompat.getString(context, R.string.project_status_archived)
        DELETED -> ContextCompat.getString(context, R.string.project_status_deleted)
        ALL -> ContextCompat.getString(context, R.string.project_status_all)
    }

    fun getStatusColor(context: Context): Int = when (this) {
        ACTIVE -> ContextCompat.getColor(context, R.color.green)
        ARCHIVED -> ContextCompat.getColor(context, R.color.gray)
        DELETED -> ContextCompat.getColor(context, R.color.red)
        else -> ContextCompat.getColor(context, R.color.networkStatusBackgroundOff)
    }

    fun getStatusIcon(): Int = when (this) {
        ACTIVE -> R.drawable.ic_status_active
        ARCHIVED -> R.drawable.ic_status_archived
        DELETED -> R.drawable.ic_status_deleted
        else -> R.drawable.ic_status_all
    }
}
