package ru.iuturakulov.mybudget.domain.repositories

import ru.iuturakulov.mybudget.data.local.daos.NotificationsDao
import ru.iuturakulov.mybudget.data.remote.FCMService
import ru.iuturakulov.mybudget.data.remote.NotificationsService
import ru.iuturakulov.mybudget.data.remote.dto.NotificationDto
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType
import ru.iuturakulov.mybudget.data.remote.dto.PreferencesRequest
import ru.iuturakulov.mybudget.domain.mappers.FCMNotificationsMapper
import javax.inject.Inject

class NotificationsRepository @Inject constructor(
    private val notificationsService: NotificationsService,
    private val notificationsDao: NotificationsDao,
    private val fcmService: FCMService,
) {

    // Overview notifications

    suspend fun getNotifications(): List<NotificationDto> {
        return notificationsService.getAllNotifications()
    }

    suspend fun markRead(id: String) {
        runCatching { notificationsService.markRead(id) }
    }

    suspend fun removeNotification(id: String) {
        runCatching { notificationsService.removeNotification(id) }
    }

    // FCM

    suspend fun getFCMNotificationPreferences(projectId: String): List<NotificationType> {
        val response = fcmService.getProjectNotificationPreferences(projectId)
        return FCMNotificationsMapper.mapToNotificationType(response.body()?.types.orEmpty())
    }

    suspend fun setFCMNotificationPreferences(projectId: String, request: PreferencesRequest) {
        fcmService.updateProjectNotificationPreferences(projectId, request)
    }
}

