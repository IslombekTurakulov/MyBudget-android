package ru.iuturakulov.mybudget.ui.projects.participants

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.DialogParticipantBinding
import java.util.UUID

@AndroidEntryPoint
class ParticipantDialogFragment : DialogFragment() {

    private var _binding: DialogParticipantBinding? = null
    private val binding get() = _binding!!

    private var participant: ParticipantEntity? = null
    private var projectId: String? = null
    private var onParticipantUpdated: ((ParticipantEntity) -> Unit)? = null
    private var onParticipantDeleted: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogParticipantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем аргументы
        participant = arguments?.getParcelable(ARG_PARTICIPANT)
        projectId = arguments?.getString(ARG_PROJECT_ID)

        // Настроить виды
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupViews() {
        participant?.let { entity ->
            binding.etParticipantName.setText(entity.name)
            binding.etParticipantEmail.setText(entity.email)
            binding.spinnerRole.setText(entity.role.name, false)
        }

        setupRoleSpinner()

        // Установка слушателей кнопок
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveParticipant()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

//        binding.btnDelete.setOnClickListener {
//            showDeleteConfirmationDialog()
//        }
    }

    private fun setupRoleSpinner() {
        // Получаем список локализованных названий ролей
        val rolesDisplayNames = ParticipantRole.entries.filter {
            it.displayNameRes != R.string.role_admin
        }.map { it.getDisplayName(requireContext()) }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            rolesDisplayNames
        )

        binding.spinnerRole.setAdapter(adapter)

        // Устанавливаем текущее значение
        participant?.let {
            val currentRole = ParticipantRole.values().find { role ->
                role.name.equals(it.role.name, ignoreCase = true)
            }
            currentRole?.let { role ->
                binding.spinnerRole.setText(role.getDisplayName(requireContext()), false)
            }
        } ?: run {
            // Значение по умолчанию
            binding.spinnerRole.setText(ParticipantRole.VIEWER.getDisplayName(requireContext()), false)
        }
    }

    private fun validateInput(): Boolean {
        clearErrors()

        val name = binding.etParticipantName.text.toString()
        val email = binding.etParticipantEmail.text.toString()
        var isValid = true

        if (email.isBlank() || !email.isValidEmail()) {
            binding.etParticipantEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (getSelectedRole() == null) {
            binding.spinnerRole.error = getString(R.string.error_select_valid_role)
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        binding.etParticipantName.error = null
        binding.etParticipantEmail.error = null
        binding.spinnerRole.error = null
    }

    private fun saveParticipant() {
        val role = getSelectedRole() ?: run {
            binding.spinnerRole.error = getString(R.string.error_select_valid_role)
            return
        }

        val name = binding.etParticipantName.text.toString()
        val email = binding.etParticipantEmail.text.toString()

        val updatedParticipant = participant?.copy(
            name = name,
            email = email,
            role = role
        ) ?: run {
            requireNotNull(projectId) { "Project ID is required for new participant" }
            ParticipantEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId!!,
                userId = "",
                name = name,
                email = email,
                role = role
            )
        }

        onParticipantUpdated?.invoke(updatedParticipant)
        dismiss()
    }

    private fun getSelectedRole(): ParticipantRole? {
        return ParticipantRole.entries.find { participantRole ->
            participantRole.getDisplayName(requireContext()) == binding.spinnerRole.text.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnParticipantUpdated(listener: (ParticipantEntity) -> Unit) {
        onParticipantUpdated = listener
    }

    fun setOnParticipantDeleted(listener: () -> Unit) {
        onParticipantDeleted = listener
    }

    private fun String.isValidEmail() = Patterns.EMAIL_ADDRESS.matcher(this).matches()

    companion object {
        private const val ARG_PARTICIPANT = "participant"
        private const val ARG_PROJECT_ID = "project_id"

        fun newInstance(participant: ParticipantEntity?, projectId: String?): ParticipantDialogFragment {
            return ParticipantDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PARTICIPANT, participant)
                    putString(ARG_PROJECT_ID, projectId)
                }
            }
        }
    }
}
