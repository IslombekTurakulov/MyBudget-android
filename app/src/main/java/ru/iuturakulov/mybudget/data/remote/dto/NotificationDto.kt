package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable

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
    val limit: Double = 0.0,
)

enum class NotificationType {
    PROJECT_INVITE_SEND,
    PROJECT_INVITE_ACCEPT,
    PARTICIPANT_ROLE_CHANGE,
    PARTICIPANT_REMOVED,
    TRANSACTION_ADDED,
    TRANSACTION_UPDATED,
    TRANSACTION_REMOVED,
    PROJECT_EDITED,
    PROJECT_REMOVED,
    PROJECT_ARCHIVED,
    PROJECT_UNARCHIVED,
    SYSTEM_ALERT,
    UNKNOWN;

    companion object {
        fun from(type: String): NotificationType {
            return entries.find { it.name.equals(type, ignoreCase = true) } ?: NotificationType.UNKNOWN
        }
    }
}
