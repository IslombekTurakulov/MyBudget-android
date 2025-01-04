package ru.iuturakulov.mybudget.ui.transactions

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction
import ru.iuturakulov.mybudget.databinding.DialogIconPickerBinding
import ru.iuturakulov.mybudget.databinding.DialogTransactionDetailsBinding
import ru.iuturakulov.mybudget.domain.mappers.CategoryIconMapper
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter

@AndroidEntryPoint
class TransactionDetailsDialogFragment : DialogFragment() {

    private var _binding: DialogTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    private var transaction: TemporaryTransaction? = null
    private var onTransactionUpdated: ((TemporaryTransaction) -> Unit)? = null
    private var onTransactionDeleted: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transaction = arguments?.getParcelable(ARG_TRANSACTION)

        setupViews()
    }

    private fun setupViews() {
        transaction?.let { trans ->
            // Устанавливаем данные в поля
            binding.etTransactionName.setText(trans.name)
            binding.etTransactionAmount.setText(trans.amount.toString())
            binding.spinnerCategory.setText(trans.category, false)
            updateCategoryIcon(trans.categoryIcon)

            // Настраиваем список категорий
            setupCategorySpinner()

            // Обработка кнопки "Сохранить"
            binding.btnEditTransaction.setOnClickListener {
                if (validateInput()) {
                    val updatedTransaction = trans.copy(
                        name = binding.etTransactionName.text.toString(),
                        amount = binding.etTransactionAmount.text.toString().toDoubleOrNull()
                            ?: trans.amount,
                        category = binding.spinnerCategory.text.toString(),
                        categoryIcon = CategoryIconMapper.getIconForCategory(binding.spinnerCategory.text.toString()),
                        date = System.currentTimeMillis()
                    )
                    onTransactionUpdated?.invoke(updatedTransaction)
                    dismiss()
                }
            }

            binding.ivTransactionCategoryIcon.setOnClickListener {
                showEmojiPickerDialog()
            }

            // Обработка кнопки "Удалить"
            binding.btnDeleteTransaction.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить транзакцию")
                    .setMessage("Вы уверены, что хотите удалить эту транзакцию?")
                    .setPositiveButton("Удалить") { _, _ ->
                        onTransactionDeleted?.invoke()
                        dismiss()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.transaction_categories)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            val selectedIcon = CategoryIconMapper.getIconForCategory(selectedCategory)
            updateCategoryIcon(selectedIcon)
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.etTransactionName.text?.toString()
        val amount = binding.etTransactionAmount.text?.toString()

        if (name.isNullOrBlank()) {
            binding.etTransactionName.error = "Введите название"
            return false
        }

        if (amount.isNullOrBlank() || amount.toDoubleOrNull() == null) {
            binding.etTransactionAmount.error = "Введите корректную сумму"
            return false
        }

        return true
    }

    private fun updateCategoryIcon(icon: String) {
        binding.ivTransactionCategoryIcon.text = icon
    }

    private fun showEmojiPickerDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = DialogIconPickerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogView.root)

        dialogView.recyclerViewEmojis.layoutManager = GridLayoutManager(requireContext(), 6)
        // Получаем список эмодзи из ресурсов
        val emojis = resources.getStringArray(R.array.emoji_list)
        val adapter = EmojiPickerAdapter(emojis.toList()) { selectedEmoji ->
            updateCategoryIcon(selectedEmoji) // Обновляем иконку
            transaction = transaction?.copy(categoryIcon = selectedEmoji) // Сохраняем выбор
            dialog.dismiss()
        }
        dialogView.recyclerViewEmojis.adapter = adapter

        dialog.show()
    }

    fun setOnTransactionUpdated(listener: (TemporaryTransaction) -> Unit) {
        onTransactionUpdated = listener
    }

    fun setOnTransactionDeleted(listener: () -> Unit) {
        onTransactionDeleted = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRANSACTION = "arg_transaction"

        fun newInstance(transaction: TemporaryTransaction): TransactionDetailsDialogFragment {
            val fragment = TransactionDetailsDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_TRANSACTION, transaction)
            fragment.arguments = args
            return fragment
        }
    }
}
