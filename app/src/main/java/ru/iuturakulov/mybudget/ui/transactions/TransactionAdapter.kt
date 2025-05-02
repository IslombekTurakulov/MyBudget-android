package ru.iuturakulov.mybudget.ui.transactions

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import ru.iuturakulov.mybudget.R
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

        fun bind(transaction: TransactionEntity) {
            binding.apply {
                tvTransactionName.text = transaction.name
                tvTransactionCategory.text = transaction.category
                tvTransactionUser.text = binding.root.context.getString(
                    R.string.transaction_author,
                    if (transaction.userName.equals("Вы", ignoreCase = true)) {
                        binding.root.context.getString(R.string.transaction_title_your)
                    } else {
                        transaction.userName
                    }
                )
                tvTransactionDate.text = transaction.date.toIso8601Date()

                val transactionType = TransactionEntity.TransactionType.fromString(transaction.type)
                tvTransactionAmount.text = formatAmount(transaction, transactionType)

                // Устанавливаем цвет суммы в зависимости от типа транзакции
                val amountColorRes = when (transactionType) {
                    TransactionEntity.TransactionType.EXPENSE -> android.R.color.holo_red_dark
                    TransactionEntity.TransactionType.INCOME -> android.R.color.holo_green_dark
                }
                tvTransactionAmount.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        amountColorRes
                    )
                )
                ivTransactionCategoryIcon.text = transaction.categoryIcon
                itemTransactionContainer.setOnClickListener { onTransactionClicked(transaction) }

                llReceipts.removeAllViews()
                val receipts = transaction.images
                if (receipts.isNotEmpty()) {
                    llReceipts.isVisible = true

                    val maxShow = 3
                    receipts.take(maxShow).forEachIndexed { idx, base64 ->
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        val iv = ImageView(root.context).apply {
                            setImageBitmap(bmp)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            clipToOutline = true
                            outlineProvider = ViewOutlineProvider.BACKGROUND
                            layoutParams = LinearLayout.LayoutParams(
                                root.context.dpToPx(48),
                                root.context.dpToPx(48)
                            ).apply {
                                if (idx > 0) leftMargin = root.context.dpToPx(4)
                            }
                        }
                        llReceipts.addView(iv)
                    }

                    if (receipts.size > maxShow) {
                        val more = receipts.size - maxShow
                        val tv = MaterialTextView(root.context).apply {
                            text = "+$more"
                            gravity = Gravity.CENTER
                            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline4)
                            background = ContextCompat.getDrawable(
                                root.context,
                                R.drawable.baseline_circle_24
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                root.context.dpToPx(48),
                                root.context.dpToPx(48)
                            ).apply {
                                leftMargin = root.context.dpToPx(4)
                            }
                        }
                        llReceipts.addView(tv)
                    }
                } else {
                    llReceipts.isGone = true
                }
            }
        }

        private fun formatAmount(
            transaction: TransactionEntity,
            type: TransactionEntity.TransactionType
        ): String {
            val formattedAmount = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                currency = Currency.getInstance("RUB")
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }.format(transaction.amount)
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

    companion object {
        fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    }
}

