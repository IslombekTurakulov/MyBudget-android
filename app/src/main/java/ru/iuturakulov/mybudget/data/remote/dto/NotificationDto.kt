package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable
import ru.iuturakulov.mybudget.data.local.entities.NotificationEntity

@Serializable
data class NotificationDto(
    val id: String,
    val userId: String,
    val projectId: String?,
    val message: String,
    val createdAt: Long,
    val isRead: Boolean = false,
    val type: NotificationType,
    val payload: NotificationPayload? = null
)

enum class NotificationType {
    PROJECT_INVITE,
    ROLE_CHANGE,
    TRANSACTION_ADDED,
    TRANSACTION_REMOVED,
    PROJECT_EDITED,
    PROJECT_REMOVED,
    SYSTEM_ALERT
}

sealed interface NotificationPayload {
    data class Project(val projectId: String) : NotificationPayload
    data class Transaction(val projectId: String, val transactionId: String) : NotificationPayload
}

