package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.local.daos.NotificationsDao
import ru.iuturakulov.mybudget.data.remote.NotificationsService
import ru.iuturakulov.mybudget.data.remote.dto.NotificationDto
import javax.inject.Inject

class NotificationsRepository @Inject constructor(
    private val notificationsService: NotificationsService,
    private val notificationsDao: NotificationsDao
) {
    suspend fun getNotifications(): List<NotificationDto> {
        return notificationsService.getAllNotifications()
    }

    suspend fun markRead(id: String) {
        runCatching { notificationsService.markRead(id) }
    }

    suspend fun removeNotification(id: String) {
        runCatching { notificationsService.removeNotification(id) }
    }
}

