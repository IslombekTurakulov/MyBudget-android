package ru.iuturakulov.mybudget.ui.projects.list

import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.databinding.DialogFilterBinding
import ru.iuturakulov.mybudget.databinding.FragmentProjectListBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.projects.create.CreateProjectFragment

@AndroidEntryPoint
class ProjectListFragment :
    BaseFragment<FragmentProjectListBinding>(R.layout.fragment_project_list) {

    private val viewModel: ProjectListViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter

    override fun getViewBinding(view: View): FragmentProjectListBinding =
        FragmentProjectListBinding.bind(view)

    @OptIn(FlowPreview::class)
    override fun setupViews() {
        adapter = ProjectAdapter { project ->
            val action =
                ProjectListFragmentDirections.actionProjectsToDetails(projectId = project.id)
            findNavController().navigate(action)
        }
        setupToolbar()
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
            val createProjectBottomSheet = CreateProjectFragment()
            createProjectBottomSheet.show(
                this.requireActivity().supportFragmentManager,
                "CreateProjectFragment"
            )
        }

        binding.fabFilterProjects.setOnClickListener {
            showFilterDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncProjects()
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
                    }

                    is UiState.Idle -> {}
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
                    Snackbar.make(binding.root, "Ошибка синхронизации", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.inviteCodeEvent.collect { success ->
                if (success is UiState.Success) {
                    Snackbar.make(binding.root, success.data ?: "Успех", Snackbar.LENGTH_SHORT)
                        .show()
                } else if (success is UiState.Error) {
                    Snackbar.make(binding.root, success.message, Snackbar.LENGTH_SHORT).show()
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
                R.id.radioActive -> ProjectStatus.ACTIVE
                R.id.radioCompleted -> ProjectStatus.COMPLETED
                else -> ProjectStatus.ALL
            }
            viewModel.setFilterStatus(filter)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menuInviteCode -> {
                        showJoinByCodeDialog()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun showJoinByCodeDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext()).apply {
            hint = "Введите код проекта"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.setTitle("Присоединиться к проекту")
            .setView(input)
            .setPositiveButton("Присоединиться") { _, _ ->
                val code = input.text.toString()
                if (code.isNotBlank()) {
                    viewModel.joinProjectByCode(code)
                } else {
                    Toast.makeText(requireContext(), "Код не может быть пустым", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
