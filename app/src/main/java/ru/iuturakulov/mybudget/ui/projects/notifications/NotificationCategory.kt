package ru.iuturakulov.mybudget.ui.projects.notifications

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType

/**
 * Категории уведомлений — для будущей секционированной UI-логики
 */
enum class NotificationCategory {
    PROJECT,       // всё, что связано с проектами
    PARTICIPANT,   // приглашения, роли
    TRANSACTION,   // транзакции
    SYSTEM         // системные, оповещения
}

/**
 * Метаданные одного типа уведомления: заголовок, подзаголовок, иконка, категория
 */
data class NotificationMeta(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int?,
    @DrawableRes val iconRes: Int,
    val category: NotificationCategory
)

/**
 * Словарь всех типов → их метаданные
 */
val metas = mapOf(
    NotificationType.PROJECT_INVITE_SEND to NotificationMeta(
        titleRes = R.string.notif_project_invite_send,
        subtitleRes = R.string.notif_project_invite_send_sub,
        iconRes = R.drawable.ic_project_invite_send,
        category = NotificationCategory.PARTICIPANT
    ),
    NotificationType.PROJECT_INVITE_ACCEPTED to NotificationMeta(
        titleRes = R.string.notif_project_invite_accepted,
        subtitleRes = R.string.notif_project_invite_accepted_sub,
        iconRes = R.drawable.ic_project_invite_accepted,
        category = NotificationCategory.PARTICIPANT
    ),
    NotificationType.PARTICIPANT_ROLE_CHANGE to NotificationMeta(
        titleRes = R.string.notif_role_change,
        subtitleRes = R.string.notif_role_change_sub,
        iconRes = R.drawable.ic_role_change,
        category = NotificationCategory.PARTICIPANT
    ),
    NotificationType.TRANSACTION_ADDED to NotificationMeta(
        titleRes = R.string.notif_tx_added,
        subtitleRes = R.string.notif_tx_added_sub,
        iconRes = R.drawable.ic_tx_add,
        category = NotificationCategory.TRANSACTION
    ),
    NotificationType.TRANSACTION_UPDATED to NotificationMeta(
        titleRes = R.string.notif_tx_updated,
        subtitleRes = R.string.notif_tx_updated_sub,
        iconRes = R.drawable.ic_tx_update,
        category = NotificationCategory.TRANSACTION
    ),
    NotificationType.TRANSACTION_REMOVED to NotificationMeta(
        titleRes = R.string.notif_tx_removed,
        subtitleRes = R.string.notif_tx_removed_sub,
        iconRes = R.drawable.ic_tx_remove,
        category = NotificationCategory.TRANSACTION
    ),
    NotificationType.PROJECT_EDITED to NotificationMeta(
        titleRes = R.string.notif_proj_edited,
        subtitleRes = R.string.notif_proj_edited_sub,
        iconRes = R.drawable.ic_proj_edit,
        category = NotificationCategory.PROJECT
    ),
    NotificationType.PROJECT_REMOVED to NotificationMeta(
        titleRes = R.string.notif_proj_removed,
        subtitleRes = R.string.notif_proj_removed_sub,
        iconRes = R.drawable.ic_proj_remove,
        category = NotificationCategory.PROJECT
    ),
    NotificationType.PROJECT_ARCHIVED to NotificationMeta(
        titleRes = R.string.notif_proj_archived,
        subtitleRes = R.string.notif_proj_archived_sub,
        iconRes = R.drawable.ic_proj_archive,
        category = NotificationCategory.PROJECT
    ),
    NotificationType.PROJECT_UNARCHIVED to NotificationMeta(
        titleRes = R.string.notif_proj_unarchived,
        subtitleRes = R.string.notif_proj_unarchived_sub,
        iconRes = R.drawable.ic_proj_unarchive,
        category = NotificationCategory.PROJECT
    ),
    NotificationType.BUDGET_THRESHOLD to NotificationMeta(
        titleRes = R.string.notif_budget_threshold,
        subtitleRes = R.string.notif_budget_threshold_sub,
        iconRes = R.drawable.ic_warning_24,
        category = NotificationCategory.TRANSACTION
    ),
    NotificationType.SYSTEM_ALERT to NotificationMeta(
        titleRes = R.string.notif_system_alert,
        subtitleRes = R.string.notif_system_alert_sub,
        iconRes = R.drawable.ic_system_alert,
        category = NotificationCategory.SYSTEM
    ),
    NotificationType.UNKNOWN to NotificationMeta(
        titleRes = R.string.notif_unknown,
        subtitleRes = null,
        iconRes = R.drawable.ic_notifications_24,
        category = NotificationCategory.SYSTEM
    )
)

/** Быстрый доступ к метаданным из любого NotificationType */
@Suppress("NOTHING_TO_INLINE")
inline fun NotificationType.meta() = metas[this]!!
inline fun NotificationType.titleRes() = meta().titleRes
inline fun NotificationType.subtitleRes() = meta().subtitleRes
inline fun NotificationType.iconRes() = meta().iconRes
inline fun NotificationType.category() = meta().category
