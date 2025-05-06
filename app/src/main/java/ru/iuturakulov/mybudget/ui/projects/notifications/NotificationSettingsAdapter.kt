package ru.iuturakulov.mybudget.ui.projects.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType
import ru.iuturakulov.mybudget.databinding.RowNotificationSettingsBinding

class NotificationSettingsAdapter(
    private val onToggle: (NotificationType, Boolean) -> Unit
) : RecyclerView.Adapter<NotificationSettingsAdapter.NotificationViewHolder>() {

    private var items: List<NotificationType> = emptyList()
    // Набор включённых типов уведомлений
    private var enabledSet: MutableSet<NotificationType> = mutableSetOf()

    /** Обновляет список включённых типов и перерисовывает адаптер */
    fun setEnabled(types: Set<NotificationType>) {
        enabledSet.clear()
        enabledSet.addAll(types)
        notifyDataSetChanged()
    }

    /** Устанавливает весь новый список типов и перерисовывает */
    fun updateItems(newItems: List<NotificationType>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = RowNotificationSettingsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val type = items[position]
        val isEnabled = enabledSet.contains(type)
        holder.bind(type, isEnabled)
    }

    override fun getItemCount(): Int = items.size

    inner class NotificationViewHolder(
        private val binding: RowNotificationSettingsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Привязывает данные типа уведомления к элементу строки */
        fun bind(type: NotificationType, isEnabled: Boolean) {
            // Иконка и заголовок/подзаголовок из метаданных
//            binding.ivIcon.setImageResource(type.iconRes())
            binding.tvTitle.setText(type.titleRes())
            type.subtitleRes()?.let { res ->
                binding.tvSubtitle.setText(res)
                binding.tvSubtitle.visibility = View.VISIBLE
            } ?: run {
                binding.tvSubtitle.visibility = View.GONE
            }

            // сначала убираем старый слушатель, потом задаём состояние и новый слушатель
            binding.switchToggle.apply {
                setOnCheckedChangeListener(null)
                isChecked = isEnabled
                setOnCheckedChangeListener { _, checked ->
                    onToggle(type, checked)
                }
            }
        }
    }
}
