package ru.iuturakulov.mybudget.ui.projects.details

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
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
import ru.iuturakulov.mybudget.ui.transactions.TransactionDetailsDialogFragment

@AndroidEntryPoint
class ProjectDetailsFragment :
    BaseFragment<FragmentProjectDetailsBinding>(R.layout.fragment_project_details) {

    private val viewModel: ProjectDetailsViewModel by viewModels()
    private val args: ProjectDetailsFragmentArgs by navArgs()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun getViewBinding(view: View): FragmentProjectDetailsBinding {
        return FragmentProjectDetailsBinding.bind(view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadProjectDetails(args.projectId)
    }

    override fun setupViews() {
        setupSwipeRefresh()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> state.data?.let { withTransactions ->
                        showProjectDetails(withTransactions)
                    }

                    is UiState.Error -> showError(state.message)
                    is UiState.Idle -> {}
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
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            inflateMenu(R.menu.project_details_menu)
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
            onTransactionClicked = { transaction -> showTransactionDetailsDialog(transaction) },
        )
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun setupListeners() {
        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        binding.btnFilterTransactions.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun navigateToEditProject() {
        val dialog = EditProjectDialogFragment()
        dialog.show(childFragmentManager, "EditProjectDialog")
    }

    private fun confirmDeleteProject() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить проект")
            .setMessage("Вы уверены, что хотите удалить этот проект?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteProject(args.projectId)
                findNavController().navigateUp()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun navigateToParticipants() {
        // TODO: fix
//        val action = ProjectDetailsFragmentDirectionsions.actionDetailsToParticipants(args.projectId)
//        findNavController().navigate(action)
    }

    private fun navigateToAnalytics() {
        // TODO: fix
//        val action = ProjectDetailsFragmentDirections.actionDetailsToAnalytics(args.projectId)
//        findNavController().navigate(action)
    }

    private fun showLoading() {
        binding.progressBarTransactions.isVisible = true
        binding.rvTransactions.isVisible = false
        binding.tvEmptyTransactions.isVisible = false
    }

    private fun showProjectDetails(project: ProjectWithTransactions) {
        binding.progressBarTransactions.isVisible = false
        binding.rvTransactions.isVisible = true

        binding.tvProjectBudget.text = "Бюджет: ${project.project.budgetLimit} ₽"
        binding.tvProjectSpent.text = "Потрачено: ${project.project.amountSpent} ₽"
        binding.tvRemainingBudget.text =
            "Осталось: ${(project.project.budgetLimit - project.project.amountSpent)} ₽"

        updateTransactions(project.transactions)
    }

    private fun showError(message: String) {
        binding.progressBarTransactions.isVisible = false
        binding.rvTransactions.isVisible = false
        binding.tvEmptyTransactions.isVisible = true
        binding.tvEmptyTransactions.text = "Ошибка: $message"
    }

    private fun updateTransactions(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) {
            binding.tvEmptyTransactions.isVisible = true
            binding.rvTransactions.isVisible = false
        } else {
            binding.tvEmptyTransactions.isVisible = false
            binding.rvTransactions.isVisible = true
            transactionAdapter.submitList(transactions)
        }
    }

    private fun showAddTransactionDialog() {
        val dialog = AddTransactionDialogFragment.newInstance(args.projectId, args.userId)
        dialog.setOnTransactionAdded { transaction ->
            viewModel.addTransaction(args.projectId, transaction.toEntity())
        }
        dialog.show(childFragmentManager, "AddTransactionDialog")
    }

    private fun showTransactionDetailsDialog(transaction: TransactionEntity) {
        val dialog = TransactionDetailsDialogFragment.newInstance(transaction.toTemporaryModel())
        dialog.setOnTransactionUpdated { updatedTransaction ->
            viewModel.updateTransaction(args.projectId, updatedTransaction.toEntity())
        }
        dialog.setOnTransactionDeleted {
            showDeleteTransactionDialog(transaction)
        }
        dialog.show(childFragmentManager, "TransactionDetailsDialog")
    }

    private fun showDeleteTransactionDialog(transaction: TransactionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить транзакцию")
            .setMessage("Вы уверены, что хотите удалить эту транзакцию?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteTransaction(args.projectId, transaction.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = DialogFilterTransactionsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogView.root)

        // TODO: получить из бэка
        val categories = listOf("Все", "Еда", "Транспорт", "Развлечения", "Прочее")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogView.spinnerCategory.adapter = adapter

        dialogView.btnApplyFilters.setOnClickListener {
            val minAmount = dialogView.etMinAmount.text.toString().toDoubleOrNull()
            val maxAmount = dialogView.etMaxAmount.text.toString().toDoubleOrNull()
            val selectedCategory =
                if (dialogView.spinnerCategory.selectedItem == "Все") null else dialogView.spinnerCategory.selectedItem.toString()

            val filter = TransactionFilter(
                category = selectedCategory,
                minAmount = minAmount,
                maxAmount = maxAmount
            )
            viewModel.applyFilter(filter)
            dialog.dismiss()
        }

        dialogView.btnClearFilters.setOnClickListener {
            viewModel.applyFilter(TransactionFilter())
            dialog.dismiss()
        }
        dialog.show()
    }
}

