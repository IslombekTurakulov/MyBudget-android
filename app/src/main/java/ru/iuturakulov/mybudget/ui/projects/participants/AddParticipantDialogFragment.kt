package ru.iuturakulov.mybudget.ui.projects.participants

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.databinding.DialogAddParticipantBinding

@AndroidEntryPoint
class AddParticipantDialogFragment : DialogFragment() {

    private var _binding: DialogAddParticipantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectParticipantsViewModel by viewModels({ requireParentFragment() })

    private var projectId: String? = null
    private var onParticipantAdded: (() -> Unit)? = null
    private val roles = listOf("Наблюдатель", "Редактор")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddParticipantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectId = arguments?.getString(ARG_PROJECT_ID)

        setupRoleSpinner()
        setupListeners()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.invitationCodeState.collect { state ->
                when (state) {
                    is ProjectParticipantsViewModel.InvitationState.Loading -> {
                        Toast.makeText(
                            context,
                            "Идет обработка со стороны сервера...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is ProjectParticipantsViewModel.InvitationState.Success -> {
                        Toast.makeText(
                            context,
                            "Приглашение отправлено на почту",
                            Toast.LENGTH_SHORT
                        ).show()
                        onParticipantAdded?.invoke()
                        dismiss()
                    }

                    is ProjectParticipantsViewModel.InvitationState.Error -> {
                        Toast.makeText(context, "Ошибка: ${state.message}", Toast.LENGTH_LONG)
                            .show()
                    }

                    else -> {
                        // no-op
                    }
                }
            }
        }
    }

    private fun setupRoleSpinner() {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.spinnerRole.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnSendInvite.setOnClickListener {
            if (validateInput()) {
                val email = binding.etParticipantEmail.text.toString().trim()
                val role = binding.spinnerRole.text.toString()

                projectId?.let { id ->
                    viewModel.sendInvitation(
                        projectId = id,
                        email = email,
                        role = role
                    )
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        val email = binding.etParticipantEmail.text?.toString()?.trim()

        if (email.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etParticipantEmail.error = "Введите корректный email"
            return false
        }

        if (binding.spinnerRole.text.isNullOrBlank()) {
            Toast.makeText(context, "Выберите роль участника", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    fun setOnParticipantAdded(listener: () -> Unit) {
        onParticipantAdded = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PROJECT_ID = "arg_project_id"

        fun newInstance(projectId: Int): AddParticipantDialogFragment {
            val fragment = AddParticipantDialogFragment()
            val args = Bundle()
            args.putInt(ARG_PROJECT_ID, projectId)
            fragment.arguments = args
            return fragment
        }
    }
}
