package ru.iuturakulov.mybudget.ui.transactions

import android.R
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction
import ru.iuturakulov.mybudget.databinding.DialogAddTransactionBinding
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter
import java.util.UUID

@AndroidEntryPoint
class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private var args: AddTransactionArgs? = null
    private var onTransactionAdded: ((TemporaryTransaction) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        args = arguments?.getParcelable(ARG_TRANSACTION)
        setupViews()
    }

    private fun setupViews() {
        args?.let { argument ->
            setupCategorySpinner()
            setupEmojiPicker()

            // Кнопка "Сохранить"
            binding.btnSave.setOnClickListener {
                if (validateInput()) {
                    val temporaryTransaction = TemporaryTransaction(
                        // projectId-userId-timestamp.hashcode
                        // TODO: подумать
                        id = "${argument.projectId}-${argument.userId}-${UUID.randomUUID()}",
                        name = binding.etTransactionName.text.toString(),
                        amount = binding.etTransactionAmount.text.toString().toDouble(),
                        category = binding.spinnerCategory.text.toString(),
                        categoryIcon = binding.ivTransactionCategoryIcon.tag?.toString() ?: "",
                        date = System.currentTimeMillis(),
                        projectId = argument.projectId,
                        userId = argument.userId
                    )
                    onTransactionAdded?.invoke(temporaryTransaction)
                    dismiss()
                }
            }

            // Кнопка "Отмена"
            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Еда", "Транспорт", "Развлечения", "Прочее")
        val adapter =
            ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            binding.ivTransactionCategoryIcon.tag = selectedCategory
        }
    }

    private fun setupEmojiPicker() {
        binding.ivTransactionCategoryIcon.setOnClickListener {
            showEmojiPickerDialog { selectedEmoji ->
                binding.ivTransactionCategoryIcon.text = selectedEmoji // Устанавливаем эмодзи
                binding.ivTransactionCategoryIcon.tag = selectedEmoji
            }
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

    private fun showEmojiPickerDialog(onEmojiSelected: (String) -> Unit) {
        val emojis = listOf("😊", "🚗", "🍕", "🎉", "💵", "📈", "🛒", "✈️")
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = EmojiPickerAdapter(emojis) { emoji ->
                onEmojiSelected(emoji)
                dialog.dismiss()
            }
        }
        dialog.setContentView(dialogView)
        dialog.show()
    }

    fun setOnTransactionAdded(listener: (TemporaryTransaction) -> Unit) {
        onTransactionAdded = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRANSACTION = "arg_transaction"

        @kotlinx.parcelize.Parcelize
        data class AddTransactionArgs(
            val projectId: String,
            val userId: String,
        ) : Parcelable

        fun newInstance(projectId: String, userId: String): AddTransactionDialogFragment {
            val fragment = AddTransactionDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_TRANSACTION, AddTransactionArgs(projectId, userId))
            fragment.arguments = args
            return fragment
        }
    }
}
