package ru.iuturakulov.mybudget.ui.projects.participants

import android.app.AlertDialog
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.databinding.FragmentProjectParticipantsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

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
        binding.toolbar.title = "Участники проекта"
    }

    private fun setupRecyclerView() {
        adapter = ParticipantsAdapter(
            onEditClick = { participant ->
                showEditParticipantDialog(participant)
            },
            onDeleteClick = { participant ->
                confirmDeleteParticipant(participant)
            }
        )
        binding.rvParticipants.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddParticipant.setOnClickListener {
            val dialog = AddParticipantDialogFragment()
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

    private fun showAddParticipantDialog() {
        val dialog = ParticipantDialogFragment.newInstance(null, args.projectId)
        dialog.setOnParticipantUpdated { newParticipant ->
            viewModel.saveParticipant(newParticipant)
        }
        dialog.show(childFragmentManager, "AddParticipantDialog")
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
            .setTitle("Удалить участника")
            .setMessage("Вы уверены, что хотите удалить участника ${participant.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteParticipant(participant)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
