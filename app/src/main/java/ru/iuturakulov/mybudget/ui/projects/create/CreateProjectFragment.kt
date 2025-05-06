package ru.iuturakulov.mybudget.ui.projects.create

import android.view.Gravity
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.databinding.FragmentProjectCreateBinding
import ru.iuturakulov.mybudget.ui.BaseBottomSheetDialogFragment
import ru.iuturakulov.mybudget.ui.projects.details.EmojiPickerAdapter
import java.util.UUID

@AndroidEntryPoint
class CreateProjectFragment :
    BaseBottomSheetDialogFragment<FragmentProjectCreateBinding>(R.layout.fragment_project_create) {

    private val viewModel: CreateProjectViewModel by viewModels()
    private var onProjectAdded: (() -> Unit)? = null

    override fun getViewBinding(view: View) = FragmentProjectCreateBinding.bind(view)


    override fun setupViews() {
        setupToolbar()
        setupCategoryPicker()
        setupEmojiPicker()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun setupCategoryPicker() {
        val categories = resources.getStringArray(R.array.project_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.spinnerCategory.setAdapter(adapter)
//        binding.spinnerCategory.setOnClickListener {
//            binding.spinnerCategory.showDropDown()
//        }
        val spinnerTilCategory = binding.tilCategory.editText as MaterialAutoCompleteTextView
        spinnerTilCategory.threshold = 1
        spinnerTilCategory.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                true
            } else {
                false
            }
        }
    }

    private fun setupEmojiPicker() {
        binding.tvCategoryIcon.setOnClickListener {
            showEmojiPickerDialog { selectedEmoji ->
                binding.tvCategoryIcon.text = selectedEmoji
                binding.tvCategoryIcon.tag = selectedEmoji
            }
        }
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        binding.tvCategoryIcon.text = emojis.shuffled().first()
    }

    private fun showEmojiPickerDialog(onEmojiSelected: (String) -> Unit) {
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        val dialog = BottomSheetDialog(requireContext())
        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = EmojiPickerAdapter(emojis) { emoji ->
                onEmojiSelected(emoji)
                dialog.dismiss()
            }
        }
        dialog.setContentView(recyclerView)
        dialog.show()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCreate.setOnClickListener {
            if (validateInputs()) {
                val project = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = binding.etProjectName.text!!.trim().toString(),
                    description = binding.etProjectDescription.text!!.trim().toString(),
                    budgetLimit = binding.etBudgetLimit.text!!.trim().toString().toDouble(),
                    amountSpent = 0.0,
                    status = ProjectStatus.ACTIVE,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    categoryIcon = binding.tvCategoryIcon.text!!.trim().toString(),
                    category = binding.spinnerCategory.text!!.trim().toString(),
                    ownerName = "",
                    ownerEmail = "",
                    ownerId = ""
                )
                viewModel.createProject(project)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var ok = true

        if (binding.etProjectName.text?.trim().isNullOrBlank()) {
            binding.tilProjectName.error = getString(R.string.error_empty_project_name)
            ok = false
        } else binding.tilProjectName.error = null

        if (binding.spinnerCategory.text?.trim().isNullOrBlank()) {
            binding.tilCategory.error = getString(R.string.error_empty_category)
            ok = false
        } else binding.tilCategory.error = null

        if (binding.tvCategoryIcon.text.isNullOrBlank()) {
            Snackbar.make(binding.root, R.string.error_empty_icon, Snackbar.LENGTH_SHORT)
                .show()
            ok = false
        }

        if (binding.etBudgetLimit.text?.trim().isNullOrBlank()) {
            binding.tilBudgetLimit.error = getString(R.string.error_empty_budget)
            ok = false
        } else binding.tilBudgetLimit.error = null

        if ((binding.etBudgetLimit.text!!.trim().toString().toDoubleOrNull() ?: 0.0) <= 0.0) {
            binding.tilBudgetLimit.error = getString(R.string.error_empty_budget_limit)
            ok = false
        } else binding.tilBudgetLimit.error = null

        return ok
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnCreate.isEnabled = false
                    }

                    is UiState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnCreate.isEnabled = true

                        onProjectAdded?.invoke()
                        dismiss()
                    }

                    is UiState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnCreate.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    else -> {}
                }
            }
        }
    }

    fun setOnProjectAdded(listener: () -> Unit) {
        onProjectAdded = listener
    }

    companion object {
        const val TAG = "CreateProjectDialog"
    }
}
