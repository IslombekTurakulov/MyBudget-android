package ru.iuturakulov.mybudget.ui.projects.list

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.databinding.DialogFilterBinding
import ru.iuturakulov.mybudget.databinding.FragmentProjectListBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.projects.ProjectListFragmentDirections

@AndroidEntryPoint
class ProjectListFragment : BaseFragment<FragmentProjectListBinding>(R.layout.fragment_project_list) {

    private val viewModel: ProjectListViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter

    override fun getViewBinding(view: View): FragmentProjectListBinding =
        FragmentProjectListBinding.bind(view)

    override fun setupViews() {
        adapter = ProjectAdapter { project ->
            val action = ProjectListFragmentDirections.actionProjectsToDetails(project.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewProjects.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.syncProjects()
        }

        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString().orEmpty()
            lifecycleScope.launch {
                flow {
                    emit(query)
                }
                    .debounce(300)
                    .collect { debouncedQuery ->
                        viewModel.setSearchQuery(debouncedQuery)
                    }
            }
        }

        binding.fabAddProject.setOnClickListener {
            findNavController().navigate(R.id.action_projects_to_create)
        }

        binding.fabFilterProjects.setOnClickListener {
            showFilterDialog()
        }
    }

    override fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.swipeRefreshLayout.isRefreshing = true
                        binding.tvEmptyPlaceholder.isVisible = false
                        binding.btnRetry.isVisible = false
                    }
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.tvEmptyPlaceholder.isVisible = state.data?.isEmpty() == true
                        binding.btnRetry.isVisible = false
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.tvEmptyPlaceholder.isVisible = false
                        binding.btnRetry.isVisible = true
                        binding.btnRetry.setOnClickListener {
                            viewModel.syncProjects()
                        }
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.filteredProjects.collect { projects ->
                adapter.submitList(projects)
                binding.tvEmptyPlaceholder.isVisible =
                    projects.isEmpty() && binding.searchEditText.text?.isNotEmpty() == true
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.syncEvent.collect { success ->
                if (success) {
                    Snackbar.make(binding.root, "Список обновлён", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Ошибка синхронизации", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showFilterDialog() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogFilterBinding.inflate(layoutInflater)

        bottomSheet.setContentView(dialogBinding.root)

        dialogBinding.radioGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            val filter = when (checkedId) {
                R.id.radioActive -> "active"
                R.id.radioCompleted -> "completed"
                else -> ""
            }
            viewModel.setFilterStatus(filter)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }
}
