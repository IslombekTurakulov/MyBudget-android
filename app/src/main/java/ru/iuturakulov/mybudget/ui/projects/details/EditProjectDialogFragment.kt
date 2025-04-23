package ru.iuturakulov.mybudget.ui.projects.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.databinding.DialogEditProjectBinding

@AndroidEntryPoint
class EditProjectDialogFragment : DialogFragment() {

    private var _binding: DialogEditProjectBinding? = null
    private val args: EditProjectDialogFragmentArgs by navArgs()
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadProjectDetails(args.projectId)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        // подписка на данные проекта
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        state.data?.project?.let { project ->
                            binding.etProjectName.setText(project.name)
                            binding.etProjectDescription.setText(project.description)
                            binding.etBudgetLimit.setText(project.budgetLimit.toString())
                        }
                    }
                    is UiState.Error -> Snackbar.make(binding.root, state.message, LENGTH_LONG).show()
                    else -> { /* Loading/Idle */ }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.updateState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.btnSave.isEnabled = false
                    }
                    is UiState.Success -> {
                        dismiss()
                    }
                    is UiState.Error -> {
                        binding.btnSave.isEnabled = true
                        // binding.progressBar.visibility
                        val rootView = requireParentFragment().requireActivity().window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
                        rootView?.let {
                            Snackbar.make(it, state.message, Snackbar.LENGTH_LONG)
                                .setAnimationMode(Snackbar.ANIMATION_MODE_FADE)
                                .show()
                        }
                    }
                    else -> Unit
                }
            }
        }

        binding.btnSave.setOnClickListener {
            if (!isInputValid()) return@setOnClickListener

            val updated = viewModel.uiState.value
                .takeIf { it is UiState.Success }
                ?.let { (it as UiState.Success).data?.project }
                ?.copy(
                    name = binding.etProjectName.text.toString(),
                    description = binding.etProjectDescription.text.toString(),
                    budgetLimit = binding.etBudgetLimit.text.toString().toDoubleOrNull() ?: 0.0
                ) ?: return@setOnClickListener

            viewModel.editProject(updated)
        }

        binding.btnCancel.setOnClickListener { dismiss() }
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
