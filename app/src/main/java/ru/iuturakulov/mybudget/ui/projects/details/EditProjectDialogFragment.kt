package ru.iuturakulov.mybudget.ui.projects.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.databinding.DialogEditProjectBinding
import ru.iuturakulov.mybudget.ui.BaseBottomSheetDialogFragment

@AndroidEntryPoint
class EditProjectDialogFragment(
    private val projectId: String
) : BaseBottomSheetDialogFragment<DialogEditProjectBinding>(R.layout.dialog_edit_project) {

    private val vm: ProjectDetailsViewModel by activityViewModels()
    private var projectTemp: ProjectEntity? = null
    private lateinit var emojiDialog: BottomSheetDialog

    override fun getViewBinding(view: View) = DialogEditProjectBinding.bind(view)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.loadProjectDetails(projectId)
        setupEmojiPicker()
        setupToolbar()
        setupCategorySpinnerClicks()
        setupButtons()
        observeProjectDetails()
        observeUpdateState()
    }


    private fun setupEmojiPicker() {
        val emojis = resources.getStringArray(R.array.emoji_list).toList()
        emojiDialog = BottomSheetDialog(requireContext()).apply {
            val rv = RecyclerView(requireContext()).apply {
                layoutManager = GridLayoutManager(context, 5)
                adapter = EmojiPickerAdapter(emojis) { emoji ->
                    binding.tvCategoryIcon.text = emoji
                    dismiss()
                }
            }
            setContentView(rv)
        }
        binding.tvCategoryIcon.setOnClickListener { emojiDialog.show() }
    }

    private fun setupCategorySpinnerClicks() {
       //  binding.tilCategory.editText?.setOnClickListener { binding.spinnerCategory.showDropDown() }
    }

    private fun setupButtons() {
        binding.apply {
            btnCancel.setOnClickListener { dismiss() }
            btnSave.setOnClickListener { onSaveClicked() }
        }
    }

    private fun observeProjectDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state ->
                    when (state) {
                        is UiState.Success -> state.data?.project?.let { bindProject(it) }
                        is UiState.Error -> showSnackbar(state.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun bindProject(project: ProjectEntity) {
        projectTemp = project

        val allLabel = getString(R.string.all)
        val projectCategory = project.category ?: allLabel

        val baseCategories = resources
            .getStringArray(R.array.project_categories)
            .filterNot { it.equals(allLabel, ignoreCase = true) }
            .distinct()

        val options = listOf(allLabel, projectCategory)
            .plus(baseCategories)
            .distinct()

        val spinner = (binding.tilCategory.editText as MaterialAutoCompleteTextView)
        spinner.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options
            )
        )
        spinner.setText(project.category, false)
        spinner.threshold = 1

        // По нажатию на крестик на клавиатуре список сворачивается
        spinner.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                true
            } else false
        }
        binding.tvCategoryIcon.text = project.categoryIcon ?: "❓"
        binding.etProjectName.setText(project.name)
        binding.etProjectDescription.setText(project.description)
        binding.etBudgetLimit.setText(project.budgetLimit.toString())
    }

    private fun observeUpdateState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.updateState.collect { state ->
                    when (state) {
                        is UiState.Loading -> binding.btnSave.isEnabled = false
                        is UiState.Success -> dismiss()
                        is UiState.Error -> {
                            binding.btnSave.isEnabled = true
                            showSnackbar(state.message)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun onSaveClicked() {
        if (!validateInputs()) return

        val newCategory = (binding.tilCategory.editText as MaterialAutoCompleteTextView)
            .text.toString()
            .takeIf { it != getString(R.string.all) }
        val newIcon = binding.tvCategoryIcon.text.toString().takeIf { it.isNotBlank() }

        val updated = projectTemp
            ?.copy(
                name = binding.etProjectName.text.toString(),
                description = binding.etProjectDescription.text.toString(),
                budgetLimit = binding.etBudgetLimit.text.toString().toDouble(),
                category = newCategory,
                categoryIcon = newIcon
            ) ?: return

        vm.editProject(updated)
    }

    private fun validateInputs(): Boolean {
        var ok = true
        binding.apply {
            if (etProjectName.text.isNullOrBlank()) {
                tilProjectName.error = getString(R.string.error_enter_name)
                ok = false
            }
            if (etBudgetLimit.text.toString().toDoubleOrNull() == null) {
                tilBudgetLimit.error = getString(R.string.error_enter_valid_amount)
                ok = false
            }
            if (binding.spinnerCategory.text?.trim().isNullOrBlank()) {
                binding.tilCategory.error = getString(R.string.error_empty_category)
                ok = false
            } else binding.tilCategory.error = null
        }
        return ok
    }

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }
}
