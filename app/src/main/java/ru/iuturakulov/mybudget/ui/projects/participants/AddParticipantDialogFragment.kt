package ru.iuturakulov.mybudget.ui.projects.participants

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.DialogAddParticipantBinding

@AndroidEntryPoint
class AddParticipantDialogFragment : DialogFragment() {

    private var _binding: DialogAddParticipantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProjectParticipantsViewModel by viewModels({ requireParentFragment() })

    private var projectId: String? = null
    private var onParticipantAdded: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddParticipantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupArguments()
        setupUI()
        setupObservers()
    }

    private fun setupArguments() {
        projectId = arguments?.getString(ARG_PROJECT_ID)?.takeIf { it.isNotBlank() }
            ?: run {
                dismiss()
                return
            }
    }

    private fun setupUI() {
        setupRoleDropdown()
        setupListeners()
    }

    private fun setupRoleDropdown() {
        val roles = ParticipantRole.values().filter {
            it.displayNameRes != R.string.role_admin
        }.map { it.getDisplayName(requireContext()) }

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
            roles
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, R.color.selector_dropdown_text))
                return view
            }
        }

        binding.spinnerRole.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnSendInvite.setOnClickListener {
            if (validateInput()) {
                sendInvitation()
            }
        }

        binding.tilParticipantEmail.setEndIconOnClickListener {
            binding.etParticipantEmail.text?.clear()
        }
    }

    private fun validateInput(): Boolean {
        return when {
            !isEmailValid() -> {
                binding.tilParticipantEmail.error = getString(R.string.error_invalid_email)
                false
            }
            !isRoleSelected() -> {
                showRoleError()
                false
            }
            else -> true
        }
    }

    private fun isEmailValid(): Boolean {
        val email = binding.etParticipantEmail.text?.toString()?.trim()
        return !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isRoleSelected(): Boolean {
        return binding.spinnerRole.text?.isNotBlank() == true
    }

    private fun showRoleError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.error_select_role),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun sendInvitation() {
        val email = binding.etParticipantEmail.text.toString().trim()
        val roleName = binding.spinnerRole.text.toString()
        val role = ParticipantRole.fromDisplayName(requireContext(), roleName)

        role?.let {
            viewModel.sendInvitation(
                projectId = requireNotNull(projectId),
                email = email,
                role = it.name
            )
        } ?: showRoleError()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.invitationState.collect { state ->
                when (state) {
                    is ProjectParticipantsViewModel.InvitationState.Loading ->
                        showLoadingState()

                    is ProjectParticipantsViewModel.InvitationState.Success ->
                        handleSuccessState()

                    is ProjectParticipantsViewModel.InvitationState.Error ->
                        showErrorState(state.message)

                    else -> Unit // no-op
                }
            }
        }
    }

    private fun showLoadingState() {
        binding.btnSendInvite.isEnabled = false
        binding.progressBar.show()
    }

    private fun handleSuccessState() {
        Toast.makeText(
            requireContext(),
            getString(R.string.invitation_sent_success),
            Toast.LENGTH_SHORT
        ).show()
        onParticipantAdded?.invoke()
        dismiss()
    }

    private fun showErrorState(message: String) {
        binding.btnSendInvite.isEnabled = true
        binding.progressBar.hide()
        Toast.makeText(
            requireContext(),
            getString(R.string.invitation_sent_error, message),
            Toast.LENGTH_LONG
        ).show()
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

        fun newInstance(projectId: String): AddParticipantDialogFragment {
            return AddParticipantDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROJECT_ID, projectId)
                }
            }
        }
    }
}