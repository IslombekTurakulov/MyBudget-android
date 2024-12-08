package ru.iuturakulov.mybudget.ui.projects.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.databinding.DialogEditProjectBinding

@AndroidEntryPoint
class EditProjectDialogFragment : DialogFragment() {

    private var _binding: DialogEditProjectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectDetailsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        val project = (viewModel.uiState.value as? UiState.Success)?.data?.project
        project?.let { projectEntity ->
            binding.etProjectName.setText(projectEntity.name)
            binding.etProjectDescription.setText(projectEntity.description)
            binding.etBudgetLimit.setText(projectEntity.budgetLimit.toString())
        }

        binding.btnSave.setOnClickListener {
            if (!isInputValid()) return@setOnClickListener

            val updatedProject = project?.copy(
                name = binding.etProjectName.text.toString(),
                description = binding.etProjectDescription.text.toString(),
                budgetLimit = binding.etBudgetLimit.text.toString().toDoubleOrNull() ?: 0.0
            )

            updatedProject?.let {
                // Отключаем кнопку, чтобы предотвратить повторные нажатия
                binding.btnSave.isEnabled = false

                viewModel.updateProject(it)

                // Включаем кнопку через 1 секунду
                lifecycleScope.launch {
                    delay(1000L) // Задержка 1 секунда
                    binding.btnSave.isEnabled = true
                }

                dismiss()
            }
        }


        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun isInputValid(): Boolean {
        if (binding.etProjectName.text.isNullOrBlank()) {
            binding.etProjectNameInputLayout.error = "Название проекта не может быть пустым"
            return false
        }
        if ((binding.etBudgetLimit.text.toString().toDoubleOrNull() ?: 0.0) < 0) {
            binding.etBudgetLimitInputLayout.error = "Бюджет не может быть отрицательным"
            return false
        }
        return true
    }
}
