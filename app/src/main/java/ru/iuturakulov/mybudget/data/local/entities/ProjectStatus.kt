package ru.iuturakulov.mybudget.data.local.entities

import android.content.Context
import androidx.core.content.ContextCompat
import ru.iuturakulov.mybudget.R

enum class ProjectStatus(val type: String) {
    ACTIVE("active"),
    COMPLETED("completed"),
    PENDING("pending"),
    CANCELLED("cancelled"),
    ALL("all");

    fun canTransitionTo(newStatus: ProjectStatus): Boolean {
        val allowedTransitions = mapOf(
            PENDING to listOf(ACTIVE, CANCELLED),
            ACTIVE to listOf(COMPLETED, CANCELLED),
            ALL to listOf(ACTIVE, PENDING, COMPLETED, CANCELLED),
            COMPLETED to emptyList<ProjectStatus>(),
            CANCELLED to emptyList<ProjectStatus>()
        )
        return allowedTransitions[this]?.contains(newStatus) == true
    }

    companion object {
        fun ProjectStatus.getStatusColor(context: Context): Int {
            return when (this) {
                ACTIVE -> ContextCompat.getColor(context, R.color.green)
                COMPLETED -> ContextCompat.getColor(context, R.color.blue)
                PENDING -> ContextCompat.getColor(context, R.color.orange)
                CANCELLED -> ContextCompat.getColor(context, R.color.red)
                ALL -> ContextCompat.getColor(context, R.color.gray)
            }
        }
    }
}
