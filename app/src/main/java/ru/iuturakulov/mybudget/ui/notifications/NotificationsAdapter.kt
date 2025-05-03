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

        fun bind(item: NotificationDto, animateReadChange: Boolean) = with(vb) {
            val dt = Instant.ofEpochMilli(item.createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault()))
            tvDate.text = dt
            tvTitle.text = item.message

            // Иконка по типу уведомления
            ivIcon.setImageResource(
                when (item.type) {
                    NotificationType.PROJECT_INVITE -> R.drawable.ic_invite
                    NotificationType.ROLE_CHANGE -> R.drawable.ic_role_change
                    NotificationType.TRANSACTION_ADDED -> R.drawable.ic_tx_added
                    NotificationType.TRANSACTION_UPDATED -> R.drawable.ic_tx_updated
                    NotificationType.TRANSACTION_REMOVED -> R.drawable.ic_tx_removed
                    NotificationType.PROJECT_EDITED -> R.drawable.ic_project_edit
                    NotificationType.PROJECT_REMOVED -> R.drawable.ic_project_remove
                    NotificationType.SYSTEM_ALERT -> R.drawable.ic_system
                    NotificationType.BUDGET_THRESHOLD -> R.drawable.ic_project_budget_limit
                }
            )

//            tvProject.text = item.projectName
            val before = item.beforeSpent
            val after = item.afterSpent
            val limit = item.limit
            // tvBudgetLine.text = budgetLine(before, after, limit)
            // рассчитываем процент для индикатора (0..100)
//            val pct = if (limit == 0.0) 0 else ((after / limit) * 100).toInt().coerceIn(0, 100)
//            progressIndicator.setProgressCompat(pct, true)

            // Индикатор непрочитанного с анимацией при смене isRead
//            unreadIndicator.isVisible = !item.isRead
//            if (animateReadChange) {
//                val targetAlpha = if (item.isRead) 0f else 1f
//                unreadIndicator.animate()
//                    .alpha(targetAlpha)
//                    .setDuration(200)
//                    .start()
//            } else {
//                // если не анимируем, сразу ставим нужную прозрачность
//                unreadIndicator.alpha = if (item.isRead) 0f else 1f
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

    // Ваша функция budgetLine в scope адаптера для краткости
    private fun budgetLine(b: Double, a: Double, limit: Double): String {
        val pb = if (limit == 0.0) 0.0 else b / limit * 100
        val pa = if (limit == 0.0) 0.0 else a / limit * 100
        return "%,.2f ₽ (%.1f %%) → %,.2f ₽ (%.1f %%)"
            .format(b, pb, a, pa)
    }
}
