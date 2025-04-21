package ru.iuturakulov.mybudget.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.iuturakulov.mybudget.ui.notifications.NotificationsViewModel
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean
)
