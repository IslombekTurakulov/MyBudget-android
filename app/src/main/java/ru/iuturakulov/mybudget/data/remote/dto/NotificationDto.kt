package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable
import ru.iuturakulov.mybudget.data.local.entities.NotificationEntity

@Serializable
data class NotificationDto(
    val id: String,
    val userId: String,
    val projectId: String? = null,
    val projectName: String = "",
    val message: String,
    val createdAt: Long,
    val isRead: Boolean = false,
    val type: NotificationType,
    val beforeSpent: Double = 0.0,
    val afterSpent: Double = 0.0,
    val limit: Double = 0.0
)

enum class NotificationType {
    PROJECT_INVITE,
    ROLE_CHANGE,
    TRANSACTION_ADDED,
    TRANSACTION_UPDATED,
    TRANSACTION_REMOVED,
    PROJECT_EDITED,
    BUDGET_THRESHOLD,
    PROJECT_REMOVED,
    SYSTEM_ALERT
}
