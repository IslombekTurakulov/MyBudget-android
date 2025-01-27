package ru.iuturakulov.mybudget.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.core.DateTimeExtension.toIso8601Date
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.databinding.ItemTransactionBinding
import kotlin.math.absoluteValue

class TransactionAdapter(
    private val onTransactionClicked: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionEntity) {
            binding.apply {
                tvTransactionName.text = transaction.name
                tvTransactionCategory.text = transaction.category
                tvTransactionUser.text =
                    "От: ${transaction.projectId}" // TODO: Заменить на имя пользователя
                tvTransactionDate.text = transaction.date.toIso8601Date()
                tvTransactionAmount.text = formatAmount(transaction.amount)

                tvTransactionAmount.setTextColor(
                    if (transaction.amount < 0)
                        ContextCompat.getColor(root.context, android.R.color.holo_red_dark)
                    else
                        ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
                )

                // Устанавливаем иконку категории
                ivTransactionCategoryIcon.text = transaction.categoryIcon

                // Обработчик нажатия на элемент списка
                root.setOnClickListener {
                    onTransactionClicked(transaction)
                }
            }
        }

        private fun formatAmount(amount: Double): String {
            return if (amount < 0) "-${amount.absoluteValue} ₽" else "+$amount ₽"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(
            oldItem: TransactionEntity,
            newItem: TransactionEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionEntity,
            newItem: TransactionEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
