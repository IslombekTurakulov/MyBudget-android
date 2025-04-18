package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable
import ru.iuturakulov.mybudget.data.local.entities.NotificationEntity

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean = false
)

fun NotificationDto.toEntity() = NotificationEntity(
    id = id, title = title, body = body, createdAt = createdAt, read = read
)
