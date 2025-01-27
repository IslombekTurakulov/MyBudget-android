package ru.iuturakulov.mybudget.ui.projects.participants

import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.databinding.DialogParticipantBinding

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
        participant = arguments?.getParcelable(ARG_PARTICIPANT)
        projectId = arguments?.getString(ARG_PROJECT_ID)

        setupViews()
    }

    private fun setupViews() {
        participant?.let { participant ->
            binding.etParticipantName.setText(participant.name)
            binding.etParticipantEmail.setText(participant.email)
            binding.spinnerRole.setText(participant.role, false)
        }

        setupRoleSpinner()

        if (participant == null) {
            binding.btnSave.setOnClickListener {
                if (validateInput()) {
                    val updatedParticipant = participant?.copy(
                        name = binding.etParticipantName.text.toString(),
                        email = binding.etParticipantEmail.text.toString(),
                        role = binding.spinnerRole.text.toString()
                    ) ?: projectId?.let { it1 ->
                        ParticipantEntity(
                            id = 0, // Новый ID будет назначен на сервере
                            projectId = it1,
                            userId = "", // Здесь может быть ID текущего пользователя, если нужно
                            name = binding.etParticipantName.text.toString(),
                            email = binding.etParticipantEmail.text.toString(),
                            role = binding.spinnerRole.text.toString()
                        )
                    }
                    updatedParticipant?.let { onParticipantUpdated?.invoke(it) }
                    dismiss()
                }
            }
        }

        if (participant != null) {
            binding.btnDelete.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить участника")
                    .setMessage("Вы уверены, что хотите удалить этого участника?")
                    .setPositiveButton("Удалить") { _, _ ->
                        onParticipantDeleted?.invoke()
                        dismiss()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
    }

    private fun setupRoleSpinner() {
        val roles = listOf("Наблюдатель", "Редактор")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.spinnerRole.setAdapter(adapter)
    }

    private fun validateInput(): Boolean {
        val name = binding.etParticipantName.text.toString()
        val email = binding.etParticipantEmail.text.toString()
        val role = binding.spinnerRole.text.toString()

        if (name.isBlank()) {
            binding.etParticipantName.error = "Введите имя"
            return false
        }

        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etParticipantEmail.error = "Введите корректный email"
            return false
        }

        if (role.isBlank()) {
            binding.spinnerRole.error = "Выберите роль"
            return false
        }

        return true
    }

    fun setOnParticipantUpdated(listener: (ParticipantEntity) -> Unit) {
        onParticipantUpdated = listener
    }

    fun setOnParticipantDeleted(listener: () -> Unit) {
        onParticipantDeleted = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PARTICIPANT = "arg_participant"
        private const val ARG_PROJECT_ID = "arg_project_id"

        fun newInstance(
            participant: ParticipantEntity?,
            projectId: String
        ): ParticipantDialogFragment {
            val fragment = ParticipantDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_PARTICIPANT, participant)
                putString(ARG_PROJECT_ID, projectId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}

