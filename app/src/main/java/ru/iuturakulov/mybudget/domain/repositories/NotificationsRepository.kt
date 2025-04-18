package ru.iuturakulov.mybudget.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.iuturakulov.mybudget.data.local.daos.NotificationsDao
import ru.iuturakulov.mybudget.data.local.entities.NotificationEntity
import ru.iuturakulov.mybudget.data.local.entities.toUi
import ru.iuturakulov.mybudget.data.remote.NotificationsService
import ru.iuturakulov.mybudget.data.remote.dto.toEntity
import ru.iuturakulov.mybudget.ui.notifications.NotificationsViewModel
import javax.inject.Inject

class NotificationsRepository @Inject constructor(
    private val notificationsService: NotificationsService,
    private val notificationsDao: NotificationsDao
) {
    fun observe(): Flow<List<NotificationsViewModel.NotificationUi>> =
        notificationsDao.observeAll().map { list -> list.map(NotificationEntity::toUi) }

    suspend fun refresh() {
        val remote = notificationsService.getAll()
        notificationsDao.clear()
        notificationsDao.upsert(remote.map { it.toEntity() })
    }

    suspend fun markRead(id: String) {
        notificationsDao.markRead(id)
        runCatching { notificationsService.markRead(id) }
    }
}


