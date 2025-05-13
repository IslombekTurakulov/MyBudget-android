package ru.iuturakulov.mybudget.ui.projects.list

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.leinardi.android.speeddial.SpeedDialActionItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.auth.CodeTokenStorage
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.databinding.FragmentProjectListBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.projects.create.CreateProjectFragment
import javax.inject.Inject

@AndroidEntryPoint
class ProjectListFragment :
    BaseFragment<FragmentProjectListBinding>(R.layout.fragment_project_list) {

    private val viewModel: ProjectListViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter
    private var joinDialog: AlertDialog? = null

    @Inject
    lateinit var codeTokenStorage: CodeTokenStorage

    override fun getViewBinding(view: View) = FragmentProjectListBinding.bind(view)

    override fun setupViews() {
        adapter = ProjectAdapter { project ->
            val action =
                ProjectListFragmentDirections.actionProjectsToDetails(projectId = project.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewProjects.adapter?.setHasStableIds(true)
        binding.recyclerViewProjects.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.syncProjects()
        }

        MutableStateFlow("")
            .also { state ->
                binding.searchEditText.doOnTextChanged { text, _, _, _ ->
                    state.value = text?.toString().orEmpty()
                }
            }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                viewModel.setSearchQuery(query)
            }
            .launchIn(lifecycleScope)

        binding.searchEditText.apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    this.clearFocus()
                    true
                } else false
            }
        }

        binding.speedDial.apply {
            addActionItem(
                SpeedDialActionItem.Builder(R.id.fab_add, R.drawable.baseline_add_24)
                    .setLabel(getString(R.string.add_project))
                    .create()
            )
            addActionItem(
                SpeedDialActionItem.Builder(R.id.fab_filter, R.drawable.baseline_filter_list_24)
                    .setLabel(getString(R.string.filters))
                    .create()
            )
            addActionItem(
                SpeedDialActionItem.Builder(R.id.fab_proceed_invitation, R.drawable.ic_email_24)
                    .setLabel(getString(R.string.join_project))
                    .create()
            )
            setOnActionSelectedListener { action ->
                when (action.id) {
                    R.id.fab_add -> {
                        navigateToAddProject()
                        true
                    }

                    R.id.fab_filter -> {
                        showFilterDialog()
                        true
                    }

                    R.id.fab_proceed_invitation -> {
                        showJoinByCodeDialog()
                        true
                    }

                    else -> false
                }
            }
        }

        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncProjects()
    }

    override fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            codeTokenStorage.getCodeTokenFlow().collect { code ->
                code ?: return@collect
                viewModel.joinProjectByCode(code)
                codeTokenStorage.clearCodeToken()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.filteredProjects.collect { projects ->
                binding.swipeRefreshLayout.isRefreshing = false
                binding.recyclerViewProjects.isVisible = projects.isNotEmpty()
                binding.tvEmptyPlaceholder.isVisible = projects.isEmpty()
                adapter.submitList(projects)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.syncEvent.collect { success ->
                if (!success) {
                    Snackbar.make(binding.root, R.string.error_sync_projects, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isSyncing.collect { isSyncing ->
                binding.swipeRefreshLayout.isRefreshing = isSyncing
            }
        }

        // Ответ на приглашение по коду
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.inviteCodeEvent.collect { event ->
                when (event) {
                    is UiState.Success -> {
                        Snackbar.make(
                            binding.root,
                            event.data ?: getString(R.string.success),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        viewModel.syncProjects()
                        joinDialog?.dismiss()
                    }

                    is UiState.Error -> {
                        Snackbar.make(
                            binding.root,
                            event.message,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        joinDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
                            ?.isEnabled = true
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun navigateToAddProject() {
        val bottomSheet = CreateProjectFragment()
        bottomSheet.setOnProjectAdded {
            binding.speedDial.close()
            viewModel.syncProjects()
        }
        bottomSheet.show(childFragmentManager, CreateProjectFragment.TAG)
    }

    private fun showFilterDialog() {
        binding.speedDial.close()
        val bottomSheet = ProjectFilterBottomSheet()
        bottomSheet.show(childFragmentManager, ProjectFilterBottomSheet.TAG)
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuInviteCode -> {
                    showJoinByCodeDialog()
                    true
                }

                R.id.menuNotifications -> {
                    findNavController().navigate(R.id.action_projects_to_notifications)
                    true
                }

                else -> false
            }
        }
    }

    private fun showJoinByCodeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_by_code, null)
        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.inputLayoutProjectCode)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.etProjectCode)

        joinDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.join_project)
            .setView(dialogView)
            .setPositiveButton(R.string.join, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        joinDialog?.show()

        joinDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.let { positiveButton ->
            positiveButton.setOnClickListener {
                val code = editText.text?.toString().orEmpty()
                if (code.isBlank()) {
                    inputLayout.error = getString(R.string.error_empty_code)
                } else {
                    inputLayout.error = null
                    positiveButton.isEnabled = false
                    viewModel.joinProjectByCode(code)
                }
            }
        }
    }
}

