package ru.iuturakulov.mybudget.ui.projects.participants

import android.app.AlertDialog
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.FragmentProjectParticipantsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import timber.log.Timber

@AndroidEntryPoint
class ProjectParticipantsFragment :
    BaseFragment<FragmentProjectParticipantsBinding>(R.layout.fragment_project_participants) {

    private val viewModel: ProjectParticipantsViewModel by viewModels()
    private val args: ProjectParticipantsFragmentArgs by navArgs()
    private lateinit var adapter: ParticipantsAdapter

    override fun getViewBinding(view: View): FragmentProjectParticipantsBinding {
        return FragmentProjectParticipantsBinding.bind(view)
    }

    override fun setupViews() {
        // Сначала загружаем локальные данные
        viewModel.syncParticipants(args.projectId)
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupListeners()
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.swipeRefreshLayout.isRefreshing = true
                        showLoading()
                    }

                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        showParticipants(state.data.orEmpty())
                    }

                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        showError(state.message)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.syncParticipants(args.projectId)
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.title = getString(R.string.project_participants)
    }

    private fun setupRecyclerView() {
        Timber.i("USERROLE ${args.userRole}")
        val participantRole = ParticipantRole.entries.find {
            it.name == args.userRole
        } ?: ParticipantRole.entries.toTypedArray()[0]

        adapter = ParticipantsAdapter(
            onEditClick = { participant ->
                showEditParticipantDialog(participant)
            },
            onDeleteClick = { participant ->
                confirmDeleteParticipant(participant)
            },
            currentUserParticipantRole = participantRole,
        )
        binding.rvParticipants.adapter = adapter
        binding.fabAddParticipant.isGone = participantRole != ParticipantRole.OWNER
    }

    private fun setupListeners() {
        binding.fabAddParticipant.setOnClickListener {
            val dialog = AddParticipantDialogFragment.newInstance(args.projectId)
            dialog.setOnParticipantAdded {
                viewModel.loadParticipants(args.projectId) // Перезагружаем участников
            }
            dialog.show(childFragmentManager, "AddParticipantDialog")
        }
    }

    private fun showParticipants(participants: List<ParticipantEntity>) {
        binding.progressBar.isVisible = false
        binding.tvEmptyParticipants.isVisible = participants.isNullOrEmpty()
        binding.rvParticipants.isVisible = true
        binding.fabAddParticipant.isVisible = args.projectStatus.equals(
            other = ProjectStatus.DELETED.type,
            ignoreCase = true
        ).not() && args.userRole == ParticipantRole.OWNER.name
        adapter.submitList(participants)
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.tvEmptyParticipants.isVisible = false
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.tvEmptyParticipants.isVisible = true
        binding.tvEmptyParticipants.text = "$message"
    }

    private fun showEditParticipantDialog(participant: ParticipantEntity) {
        val dialog = ParticipantDialogFragment.newInstance(participant, args.projectId)
        dialog.setOnParticipantUpdated { updatedParticipant ->
            viewModel.saveParticipant(updatedParticipant)
        }
        dialog.setOnParticipantDeleted {
            participant.let { viewModel.deleteParticipant(it) }
        }
        dialog.show(childFragmentManager, "EditParticipantDialog")
    }

    private fun confirmDeleteParticipant(participant: ParticipantEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_participant_title)
            .setMessage(
                getString(
                    R.string.confirm_delete_participant_message,
                    participant.name
                )
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteParticipant(participant)
                viewModel.syncParticipants(args.projectId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

}
