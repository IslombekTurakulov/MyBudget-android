package ru.iuturakulov.mybudget.ui.projects.details

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.BooleanExtension.ifTrue
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectWithTransactions
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction.Companion.toEntity
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity.Companion.toTemporaryModel
import ru.iuturakulov.mybudget.databinding.DialogFilterTransactionsBinding
import ru.iuturakulov.mybudget.databinding.FragmentProjectDetailsBinding
import ru.iuturakulov.mybudget.domain.models.TransactionFilter
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.transactions.AddTransactionDialogFragment
import ru.iuturakulov.mybudget.ui.transactions.TransactionAdapter

@AndroidEntryPoint
class ProjectDetailsFragment :
    BaseFragment<FragmentProjectDetailsBinding>(R.layout.fragment_project_details) {

    private val viewModel: ProjectDetailsViewModel by viewModels()
    private val args: ProjectDetailsFragmentArgs by navArgs()
    private var isTextExpanded = false
    private lateinit var transactionAdapter: TransactionAdapter

    override fun getViewBinding(view: View): FragmentProjectDetailsBinding =
        FragmentProjectDetailsBinding.bind(view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadProjectDetails(args.projectId)
    }

    override fun setupViews() {
        setupSwipeRefresh()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        binding.btnToggleDescription.setOnClickListener { toggleTextView() }
        updateTextView()
    }

    private fun toggleTextView() {
        isTextExpanded = !isTextExpanded
        updateTextView()
    }

    private fun updateTextView() {
        binding.apply {
            if (isTextExpanded) {
                tvProjectDescription.maxLines = Int.MAX_VALUE
                btnToggleDescription.text = getString(R.string.show_less)
                btnToggleDescription.setIconResource(R.drawable.baseline_expand_less_24)
            } else {
                tvProjectDescription.maxLines = 3
                btnToggleDescription.text = getString(R.string.show_more)
                btnToggleDescription.setIconResource(R.drawable.baseline_expand_more_24)
            }
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        showContent()
                        state.data?.let { projectWithTransactions ->
                            showProjectDetails(projectWithTransactions)
                        }
                    }
                    is UiState.Error -> {
                        showContent()
                        showError(state.message)
                    }
                    is UiState.Idle -> Unit
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.filteredTransactions.collect { transactions ->
                updateTransactions(transactions)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.syncProjectDetails(args.projectId)
            // Сброс состояния будет выполнен после обновления данных в наблюдателе
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menuEdit -> {
                        navigateToEditProject()
                        true
                    }
                    R.id.menuDelete -> {
                        confirmDeleteProject()
                        true
                    }
                    R.id.menuParticipants -> {
                        navigateToParticipants()
                        true
                    }
                    R.id.menuAnalytics -> {
                        navigateToAnalytics()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClicked = { transaction ->
                showTransactionDetailsDialog(transaction)
            }
        )
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun setupListeners() {
        binding.apply {
            fabAddTransaction.setOnClickListener { showAddTransactionDialog() }
            btnFilterTransactions.setOnClickListener { showFilterDialog() }
        }
    }

    private fun navigateToEditProject() {
        val action = ProjectDetailsFragmentDirections.actionProjectToEdit(args.projectId)
        findNavController().navigate(action)
    }

    private fun confirmDeleteProject() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_project))
            .setMessage(getString(R.string.delete_project_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteProject(args.projectId)
                viewModel.syncProjects()
                findNavController().navigateUp()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun navigateToParticipants() {
        val action = ProjectDetailsFragmentDirections.actionDetailsToParticipants(args.projectId)
        findNavController().navigate(action)
    }

    private fun navigateToAnalytics() {
        val action = ProjectDetailsFragmentDirections.actionDetailsToAnalytics(args.projectId)
        findNavController().navigate(action)
    }

    private fun showLoading() {
        binding.apply {
            progressBarTransactions.isVisible = true
            rvTransactions.isVisible = false
            tvEmptyTransactions.isVisible = false
        }
    }

    private fun showContent() {
        binding.apply {
            progressBarTransactions.isVisible = false
            swipeRefreshLayout.isRefreshing = false
            rvTransactions.isVisible = true
        }
    }

    private fun showProjectDetails(projectWithTransactions: ProjectWithTransactions) {
        binding.apply {
            showContent()
            toolbar.subtitle = projectWithTransactions.project.name
            tvProjectBudget.text = getString(R.string.project_budget, projectWithTransactions.project.budgetLimit)
            tvProjectSpent.text = getString(R.string.project_spent, projectWithTransactions.project.amountSpent)
            tvRemainingBudget.text = getString(
                R.string.project_remaining,
                projectWithTransactions.project.budgetLimit - projectWithTransactions.project.amountSpent
            )

            // Показываем описание только если оно не пустое
            projectWithTransactions.project.description.isNullOrEmpty().not().ifTrue {
                val description = projectWithTransactions.project.description ?: run {
                    projectDescriptionCard.isVisible = false
                    return@ifTrue
                }
                projectDescriptionCard.isVisible = true
                tvProjectDescription.text = description
                btnToggleDescription.isVisible = description.length >= 300
            }
            updateTransactions(projectWithTransactions.transactions)
        }
    }

    private fun showError(message: String) {
        binding.progressBarTransactions.isVisible = false
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun updateTransactions(transactions: List<TransactionEntity>) {
        binding.apply {
            if (transactions.isEmpty()) {
                ivEmptyTransactions.isVisible = true
                tvEmptyTransactions.apply {
                    isVisible = true
                    text = getString(R.string.no_transactions)
                }
                rvTransactions.isVisible = false
            } else {
                tvEmptyTransactions.isVisible = false
                ivEmptyTransactions.isVisible = false
                rvTransactions.isVisible = true
                transactionAdapter.submitList(transactions)
            }
        }
    }

    private fun showAddTransactionDialog() {
        val dialog = AddTransactionDialogFragment.newInstance(args.projectId)
        dialog.setOnTransactionAdded { transaction ->
            viewModel.addTransaction(args.projectId, transaction.toEntity())
        }
        dialog.show(childFragmentManager, "AddTransactionDialog")
    }

    private fun showTransactionDetailsDialog(transaction: TransactionEntity) {
        val dialog = AddTransactionDialogFragment.newInstance(
            projectId = args.projectId,
            transaction = transaction.toTemporaryModel()
        )
        dialog.setOnTransactionUpdated { updatedTransaction ->
            viewModel.updateTransaction(args.projectId, updatedTransaction.toEntity())
        }
        dialog.setOnTransactionDeleted {
            viewModel.deleteTransaction(args.projectId, transaction.id)
        }
        dialog.show(childFragmentManager, "TransactionDetailsDialog")
    }

    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogFilterTransactionsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Получаем список категорий из ресурсов. Если нужно заменить "Все", можно использовать ресурс
        val categories = resources.getStringArray(R.array.transaction_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerCategory.setAdapter(adapter)

        // Предположим, что viewModel.currentFilter хранит выбранный ранее фильтр.
        val currentFilter = viewModel.currentFilter.value
        dialogBinding.etMinAmount.setText(currentFilter.minAmount?.toString() ?: "")
        dialogBinding.etMaxAmount.setText(currentFilter.maxAmount?.toString() ?: "")
        // Если категория не выбрана, устанавливаем значение "Все"
        val defaultValue = currentFilter.category ?: getString(R.string.all)
        val index = adapter.getPosition(defaultValue)
        dialogBinding.spinnerCategory.setSelection(index)

        dialogBinding.btnApplyFilters.setOnClickListener {
            val minAmount = dialogBinding.etMinAmount.text.toString().toDoubleOrNull()
            val maxAmount = dialogBinding.etMaxAmount.text.toString().toDoubleOrNull()
            val selectedCategory = dialogBinding.spinnerCategory.selectedItem.toString().let { category ->
                if (category == getString(R.string.all)) null else category
            }
            val filter = TransactionFilter(
                category = selectedCategory,
                minAmount = minAmount,
                maxAmount = maxAmount
            )
            // Применяем фильтр и сохраняем его в viewModel
            viewModel.applyFilter(filter)
            dialog.dismiss()
        }

        dialogBinding.btnClearFilters.setOnClickListener {
            viewModel.applyFilter(TransactionFilter())
            dialog.dismiss()
        }
        dialog.show()
    }
}
