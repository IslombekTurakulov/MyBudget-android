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
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class TransactionAdapter(
    private val onTransactionClicked: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding, onTransactionClicked)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onTransactionClicked: (TransactionEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU")).apply {
                currency = Currency.getInstance("RUB")
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
        }

        fun bind(transaction: TransactionEntity) {
            binding.apply {
                tvTransactionName.text = transaction.name
                tvTransactionCategory.text = transaction.category
                tvTransactionUser.text = "От: ${transaction.userId}"
                tvTransactionDate.text = transaction.date.toIso8601Date()

                val transactionType = TransactionEntity.TransactionType.fromString(transaction.type)
                tvTransactionAmount.text = formatAmount(transaction, transactionType)

                // Устанавливаем цвет суммы в зависимости от типа транзакции
                val amountColorRes = when (transactionType) {
                    TransactionEntity.TransactionType.EXPENSE -> android.R.color.holo_red_dark
                    TransactionEntity.TransactionType.INCOME -> android.R.color.holo_green_dark
                }
                tvTransactionAmount.setTextColor(ContextCompat.getColor(root.context, amountColorRes))
                ivTransactionCategoryIcon.text = transaction.categoryIcon

                root.setOnClickListener { onTransactionClicked(transaction) }
            }
        }

        private fun formatAmount(
            transaction: TransactionEntity,
            type: TransactionEntity.TransactionType
        ): String {
            val formattedAmount = currencyFormatter.format(transaction.amount)
            return when (type) {
                TransactionEntity.TransactionType.EXPENSE -> "-$formattedAmount"
                TransactionEntity.TransactionType.INCOME -> "+$formattedAmount"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(
            oldItem: TransactionEntity,
            newItem: TransactionEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: TransactionEntity,
            newItem: TransactionEntity
        ): Boolean = oldItem == newItem
    }
}

