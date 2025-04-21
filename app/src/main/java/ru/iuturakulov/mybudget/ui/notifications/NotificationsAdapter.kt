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
        holder.bind(item, animateReadChange = payloads.isNotEmpty())
    }

    inner class NotificationViewHolder(
        private val vb: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(vb.root) {

        fun bind(item: NotificationDto, animateReadChange: Boolean) = with(vb) {
            val dt = Instant.ofEpochMilli(item.createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault()))

            tvDate.text = dt
            tvTitle.text = item.message

//            ivIcon.setImageResource(
//                when (item.type) {
//                    NotificationType.PROJECT_INVITE -> R.drawable.ic_invite
//                    NotificationType.ROLE_CHANGE -> R.drawable.ic_role_change
//                    NotificationType.TRANSACTION_ADDED -> R.drawable.ic_tx_added
//                    NotificationType.TRANSACTION_REMOVED -> R.drawable.ic_tx_removed
//                    NotificationType.PROJECT_EDITED -> R.drawable.ic_project_edit
//                    NotificationType.PROJECT_REMOVED -> R.drawable.ic_project_remove
//                    NotificationType.SYSTEM_ALERT -> R.drawable.ic_system
//                }
//            )

//            unreadIndicator.isVisible = !item.isRead
//            if (animateReadChange) {
//                val alphaTo = if (item.isRead) 0f else 1f
//                unreadIndicator.animate()
//                    .alpha(alphaTo)
//                    .setDuration(200)
//                    .start()
//            }

            root.setOnClickListener { onClick(item) }
            root.setOnLongClickListener { onLong(item); true }
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