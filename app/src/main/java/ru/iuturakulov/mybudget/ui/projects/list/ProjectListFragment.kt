package ru.iuturakulov.mybudget.ui.projects.list

import android.view.View
import android.view.inputmethod.EditorInfo
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.databinding.FragmentProjectListBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.projects.create.CreateProjectFragment

@AndroidEntryPoint
class ProjectListFragment :
    BaseFragment<FragmentProjectListBinding>(R.layout.fragment_project_list) {

    private val viewModel: ProjectListViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter

    override fun getViewBinding(view: View) = FragmentProjectListBinding.bind(view)

    override fun setupViews() {
        adapter = ProjectAdapter { project ->
            val action =
                ProjectListFragmentDirections.actionProjectsToDetails(projectId = project.id)
            findNavController().navigate(action)
        }
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
        lifecycleScope.launchWhenStarted {
            viewModel.inviteCodeEvent.collect { event ->
                when (event) {
                    is UiState.Success -> {
                        Snackbar.make(binding.root, event.data ?: getString(R.string.success), Snackbar.LENGTH_SHORT)
                            .show()
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToAddProject() {
        val bottomSheet = CreateProjectFragment()
        bottomSheet.setOnProjectAdded { viewModel.syncProjects() }
        bottomSheet.show(childFragmentManager, CreateProjectFragment.TAG)
    }

    private fun showFilterDialog() {
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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.join_project)
            .setView(dialogView)
            .setPositiveButton(R.string.join) { dialog, _ ->
                val code = editText.text?.toString().orEmpty()
                if (code.isBlank()) {
                    inputLayout.error = getString(R.string.error_empty_code)
                } else {
                    inputLayout.error = null
                    viewModel.joinProjectByCode(code)
                    viewModel.syncProjects()
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

