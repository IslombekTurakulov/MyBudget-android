package ru.iuturakulov.mybudget.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.remote.dto.NotificationDto
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType
import ru.iuturakulov.mybudget.databinding.ItemNotificationBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationsAdapter(
    private val onClick: (NotificationDto) -> Unit,
    private val onLong: (NotificationDto) -> Unit
) : ListAdapter<NotificationDto, NotificationsAdapter.NotificationViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val vb = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(vb)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position), animateReadChange = true)
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)
        holder.bind(item, animateReadChange = payloads.contains(PAYLOAD_READ))
    }

    inner class NotificationViewHolder(
        private val vb: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(vb.root) {

        init {
            vb.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (!item.isRead) {
                        onClick(item)
                    } else {
                        // Если уведомление уже прочитано, все равно обрабатываем клик
                        // для навигации к проекту или другого действия
                        handleReadNotificationClick(item)
                    }
                }
            }
            
            vb.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLong(getItem(position))
                    true
                } else false
            }
        }

        private fun handleReadNotificationClick(item: NotificationDto) {
            // Обработка клика по прочитанному уведомлению
            when (item.type) {
                NotificationType.PROJECT_INVITE_SEND,
                NotificationType.PROJECT_INVITE_ACCEPT,
                NotificationType.PARTICIPANT_ROLE_CHANGE,
                NotificationType.PARTICIPANT_REMOVED,
                NotificationType.PROJECT_EDITED,
                NotificationType.PROJECT_REMOVED,
                NotificationType.PROJECT_ARCHIVED,
                NotificationType.PROJECT_UNARCHIVED -> {
                    item.projectId?.let { onClick(item) }
                }
                NotificationType.TRANSACTION_ADDED,
                NotificationType.TRANSACTION_UPDATED,
                NotificationType.TRANSACTION_REMOVED -> {
                    item.projectId?.let { onClick(item) }
                }
                NotificationType.SYSTEM_ALERT -> {
                    onClick(item)
                }
                else -> {
                    // Для неизвестных типов просто вызываем onClick
                    onClick(item)
                }
            }
        }

        fun bind(item: NotificationDto, animateReadChange: Boolean) = with(vb) {
            val dt = Instant.ofEpochMilli(item.createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault()))
            tvDate.text = dt
            tvTitle.text = item.message
            tvProject.text = item.projectName?.let { "Проект: $it" }
            tvProject.isVisible = item.projectName != null

            ivIcon.setImageResource(
                when (item.type) {
                    NotificationType.PROJECT_INVITE_SEND -> R.drawable.ic_notification_invite
                    NotificationType.PARTICIPANT_ROLE_CHANGE -> R.drawable.ic_notification_role
                    NotificationType.TRANSACTION_ADDED -> R.drawable.ic_notification_transaction_add
                    NotificationType.TRANSACTION_UPDATED -> R.drawable.ic_notification_transaction_add
                    NotificationType.TRANSACTION_REMOVED -> R.drawable.ic_notification_transaction_remove
                    NotificationType.PROJECT_EDITED -> R.drawable.ic_notification_project_edit
                    NotificationType.PROJECT_REMOVED -> R.drawable.ic_notification_project_remove
                    NotificationType.SYSTEM_ALERT -> R.drawable.ic_notification_system
                    else -> R.drawable.ic_notification_system
                }
            )

            // Индикатор непрочитанного с анимацией при смене isRead
            unreadIndicator.isVisible = !item.isRead
            if (animateReadChange) {
                val targetAlpha = if (item.isRead) 0f else 1f
                unreadIndicator.animate()
                    .alpha(targetAlpha)
                    .setDuration(200)
                    .start()
            } else {
                unreadIndicator.alpha = if (item.isRead) 0f else 1f
            }

            root.isClickable = true
            root.isFocusable = true
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id.hashCode().toLong()

    companion object {
        private const val PAYLOAD_READ = "payload_read"
        private val DIFF = object : DiffUtil.ItemCallback<NotificationDto>() {
            override fun areItemsTheSame(o: NotificationDto, n: NotificationDto) = o.id == n.id
            override fun areContentsTheSame(o: NotificationDto, n: NotificationDto) = o == n
            override fun getChangePayload(o: NotificationDto, n: NotificationDto): Any? {
                return if (o.isRead != n.isRead) PAYLOAD_READ else null
            }
        }
    }
}
