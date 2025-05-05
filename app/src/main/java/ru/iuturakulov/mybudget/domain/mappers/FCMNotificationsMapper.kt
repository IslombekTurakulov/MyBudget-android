package ru.iuturakulov.mybudget.domain.mappers

import ru.iuturakulov.mybudget.data.remote.dto.NotificationType

object FCMNotificationsMapper {

    fun mapToNotificationType(list: List<String>): List<NotificationType> {
        return list.map { NotificationType.from(it) }
    }
}