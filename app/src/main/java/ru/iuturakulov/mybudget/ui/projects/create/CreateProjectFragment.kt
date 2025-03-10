package ru.iuturakulov.mybudget.ui.projects.create

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.databinding.FragmentProjectCreateBinding
import ru.iuturakulov.mybudget.ui.BaseBottomSheetDialogFragment
import java.util.UUID

@AndroidEntryPoint
class CreateProjectFragment :
    BaseBottomSheetDialogFragment<FragmentProjectCreateBinding>(R.layout.fragment_project_create) {

    private val viewModel: CreateProjectViewModel by viewModels()

    override fun getViewBinding(view: View): FragmentProjectCreateBinding {
        return FragmentProjectCreateBinding.bind(view)
    }

    override fun setupViews() {
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            if (validateInputs()) {
                val name = binding.etProjectName.text.toString()
                val description = binding.etProjectDescription.text.toString()
                val budgetLimit = binding.etBudgetLimit.text.toString().toDoubleOrNull() ?: 0.0

                val project = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    budgetLimit = budgetLimit,
                    amountSpent = 0.0,
                    status = ProjectStatus.ACTIVE,
                    createdDate = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )

                viewModel.createProject(project)
            }
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> handleSuccess()
                    is UiState.Error -> showError(state.message)
                    is UiState.Idle -> {}
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.etProjectName.text.isNullOrBlank()) {
            binding.etProjectName.error = "Введите название проекта"
            isValid = false
        }

        if (binding.etBudgetLimit.text.isNullOrBlank()) {
            binding.etBudgetLimit.error = "Введите лимит бюджета"
            isValid = false
        }

        return isValid
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.btnCreate.isEnabled = false
    }

    private fun handleSuccess() {
        binding.progressBar.isVisible = false
        dismiss()
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.btnCreate.isEnabled = true
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
