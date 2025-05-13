package ru.iuturakulov.mybudget.ui.projects.details

import android.os.Bundle
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.CurrencyFormatter
import ru.iuturakulov.mybudget.core.CurrencyFormatter.roundTo
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.data.local.entities.ProjectWithTransactions
import ru.iuturakulov.mybudget.data.local.entities.TemporaryTransaction.Companion.toEntity
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.DialogFilterTransactionsBinding
import ru.iuturakulov.mybudget.databinding.FragmentProjectDetailsBinding
import ru.iuturakulov.mybudget.domain.models.TransactionFilter
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.projects.list.ProjectFilterBottomSheet
import ru.iuturakulov.mybudget.ui.projects.notifications.ProjectNotificationsBottomSheet
import ru.iuturakulov.mybudget.ui.transactions.AddTransactionDialogFragment
import ru.iuturakulov.mybudget.ui.transactions.TransactionAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProjectDetailsFragment :
    BaseFragment<FragmentProjectDetailsBinding>(R.layout.fragment_project_details) {

    private val viewModel: ProjectDetailsViewModel by viewModels()
    private val args by navArgs<ProjectDetailsFragmentArgs>()

    private var isTextExpanded = false
    private var isBudgetExpanded = true
    private lateinit var transactionAdapter: TransactionAdapter
    private var projectData: ProjectWithTransactions? = null
    private var pickedStart: Long? = null
    private var pickedEnd: Long? = null
    private var isSyncSliderDisabled = false

    override fun getViewBinding(view: View) =
        FragmentProjectDetailsBinding.bind(view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            isTextExpanded = it.getBoolean(KEY_TEXT_EXPANDED, false)
            isBudgetExpanded = it.getBoolean(KEY_BUDGET_EXPANDED, true)
        }
        viewModel.loadProjectDetails(args.projectId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_TEXT_EXPANDED, isTextExpanded)
        outState.putBoolean(KEY_BUDGET_EXPANDED, isBudgetExpanded)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        observeViewModel()
    }

    private fun setupUi() = binding.apply {
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        toolbar.setOnMenuItemClickListener(::onMenuItemClicked)

        swipeRefreshLayout.setOnRefreshListener { refreshData() }

        setupRecyclerView()

        btnToggleDescription.setOnClickListener { toggleDescription() }
        tvProjectDescription.setOnClickListener { toggleDescription() }

        layoutBudgetHeader.setOnClickListener { toggleBudgetDetails() }

        layoutBudgetInfo.setOnClickListener {
            val budget = projectData?.project?.budgetLimit ?: 0.0
            val spent = projectData?.project?.amountSpent ?: 0.0
            val remaining = budget - spent
            val msg = getString(
                R.string.budget_progress_tooltip,
                CurrencyFormatter.format(budget),
                CurrencyFormatter.format(spent),
                CurrencyFormatter.format(remaining)
            )
            TooltipCompat.setTooltipText(progressBudget, msg)
            true
        }

        btnFilterTransactions.setOnClickListener { showFilterDialog() }
        fabAddTransaction.setOnClickListener { openAddTransactionDialog() }

        rvTransactions.addOnScrollListener(scrollListener)
    }

    private fun setupRecyclerView() = binding.rvTransactions.apply {
        transactionAdapter = TransactionAdapter(::onTransactionClicked, ::onTransactionDelete)
        adapter = transactionAdapter
        setHasFixedSize(true)
        itemAnimator = DefaultItemAnimator().apply {
            addDuration = 120L
            removeDuration = 120L
            moveDuration = 120L
            changeDuration = 120L
        }
        layoutAnimation =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val role = projectData?.currentRole ?: return
            if (role == ParticipantRole.VIEWER || projectData?.project?.status == ProjectStatus.DELETED) return

            if (dy > 0 && binding.fabAddTransaction.isShown) {
                binding.fabAddTransaction.hide()
            } else if (dy < 0 && !binding.fabAddTransaction.isShown) {
                binding.fabAddTransaction.show()
            }
            if (!recyclerView.canScrollVertically(-1)) {
                binding.fabAddTransaction.show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collectLatest(::handleUiState) }
                launch { viewModel.filteredTransactions.collectLatest(::updateTransactions) }
            }
        }
    }

    private fun handleUiState(state: UiState<ProjectWithTransactions>) = binding.apply {
        swipeRefreshLayout.isRefreshing = state is UiState.Loading
        progressBarTransactions.isVisible = state is UiState.Loading

        when (state) {
            is UiState.Success -> {
                projectData = state.data
                state.data?.let { renderProjectDetails(it) }
            }

            is UiState.Error -> showError(state.message)
            else -> Unit
        }
    }

    private fun renderProjectDetails(data: ProjectWithTransactions) = binding.apply {
        toolbar.title = data.project.name
        toolbar.subtitle = data.project.status.getStatusText(requireContext()).also {
            toolbar.setSubtitleTextColor(
                data.project.status.getStatusColor(requireContext())
            )
        }

        processUserRole(data.currentRole, data.project.status)
        updateMenuItems(data.currentRole, data.project.status)

        tvProjectBudget.text = CurrencyFormatter.format(data.project.budgetLimit)
        tvProjectSpent.text = CurrencyFormatter.format(data.project.amountSpent)
        tvRemainingBudget.text = CurrencyFormatter.format(data.project.budgetLimit - data.project.amountSpent)

        val budget = data.project.budgetLimit
        val spent = data.project.amountSpent
        val progress = if (budget > 0) ((spent / budget) * 100).coerceIn(0.0, 100.0) else 0.0
        progressBudget.setProgress(progress.toInt(), true)

        data.project.description?.takeIf(String::isNotBlank)?.let { desc ->
            projectDescriptionCard.isVisible = true
            tvProjectDescription.text = desc
            btnToggleDescription.isVisible = desc.length >= MIN_DESC_LENGTH
            tvProjectDescription.maxLines = if (isTextExpanded) Int.MAX_VALUE else MAX_COLLAPSED_LINES
            btnToggleDescription.text = getString(
                if (isTextExpanded) R.string.show_less else R.string.show_more
            )
            btnToggleDescription.setIconResource(
                if (isTextExpanded) R.drawable.baseline_expand_less_24 else R.drawable.baseline_expand_more_24
            )
        } ?: run {
            projectDescriptionCard.isGone = true
        }

        layoutBudgetDetails.visibility = if (isBudgetExpanded) View.VISIBLE else View.GONE
        ivBudgetExpand.setImageResource(
            if (isBudgetExpanded) R.drawable.baseline_expand_less_24 else R.drawable.baseline_expand_more_24
        )
    }

    private fun processUserRole(role: ParticipantRole?, status: ProjectStatus) = binding.apply {
        val isProjectDeleted = status == ProjectStatus.DELETED
        toolbar.menu.findItem(R.id.menuEdit).isVisible = role == ParticipantRole.OWNER && !isProjectDeleted
        toolbar.menu.setGroupVisible(
            R.id.group_editor,
            role == ParticipantRole.OWNER && !isProjectDeleted
        )
        fabAddTransaction.isVisible = role != ParticipantRole.VIEWER && !isProjectDeleted
        toolbar.menu.findItem(R.id.menuProjectNotifications).isVisible = status != ProjectStatus.DELETED
    }

    private fun updateMenuItems(role: ParticipantRole?, status: ProjectStatus) =
        binding.toolbar.apply {
            menu.findItem(R.id.menuArchiveProject).isVisible =
                role == ParticipantRole.OWNER && status != ProjectStatus.ARCHIVED && status != ProjectStatus.DELETED
            menu.findItem(R.id.menuUnarchiveProject).isVisible =
                role == ParticipantRole.OWNER && status == ProjectStatus.ARCHIVED && status != ProjectStatus.DELETED
        }

    private fun updateTransactions(list: List<TransactionEntity>) = binding.apply {
        if (list.isEmpty()) {
            emptyTransactionsLayout.isVisible = true
            rvTransactions.isGone = true
            val isFilterActive = !viewModel.currentFilter.value.isEmpty()
            binding.emptyTransactionsText.text = if (isFilterActive) {
                getString(R.string.no_transactions_for_filter)
            } else {
                getString(R.string.no_transactions)
            }
        } else {
            emptyTransactionsLayout.isGone = true
            rvTransactions.isVisible = true
            transactionAdapter.submitList(list)
            rvTransactions.scheduleLayoutAnimation()
        }
    }

    private fun toggleDescription() {
        isTextExpanded = !isTextExpanded
        TransitionManager.beginDelayedTransition(binding.projectDescriptionCard)
        projectData?.let { renderProjectDetails(it) }
    }

    private fun toggleBudgetDetails() {
        isBudgetExpanded = !isBudgetExpanded
        TransitionManager.beginDelayedTransition(binding.projectBudgetCard)
        renderProjectDetails(projectData ?: return)
    }

    private fun showFilterDialog() {
        val dlg = DialogFilterTransactionsBinding.inflate(layoutInflater)
        val currentF = viewModel.currentFilter.value.copy()

        setupCategorySpinner(dlg, currentF)
        setupUserSpinner(dlg, currentF)
        setupTypeToggle(dlg, currentF)
        setupDateFields(dlg, currentF)
        setupAmountFields(dlg, currentF)

        val dialog = createFilterDialog(dlg)
        setupDialogButtons(dialog, dlg)
    }

    private fun setupCategorySpinner(dlg: DialogFilterTransactionsBinding, currentF: TransactionFilter) {
        val rawCats = resources.getStringArray(R.array.transaction_categories)
            .filter { it != getString(R.string.all) }
        val dynCats = viewModel.transactions.value
            .mapNotNull { it.category.takeIf(String::isNotBlank) }
        val cats = listOf(getString(R.string.all)) + (rawCats + dynCats).distinct()
        dlg.spinnerCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            cats
        )
        val catSel = currentF.category ?: getString(R.string.all)
        dlg.spinnerCategory.setSelection(cats.indexOf(catSel).coerceAtLeast(0))
    }

    private fun setupUserSpinner(dlg: DialogFilterTransactionsBinding, currentF: TransactionFilter) {
        val users = listOf(getString(R.string.all)) + viewModel.transactions.value
            .mapNotNull { it.userName.takeIf(String::isNotBlank) }
            .map { name ->
                if (name.equals("Вы", ignoreCase = true)) {
                    getString(R.string.transaction_title_your)
                } else {
                    name
                }
            }
            .distinct()
        dlg.spinnerUser.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            users
        )
        val userSel = currentF.userName ?: getString(R.string.all)
        dlg.spinnerUser.setSelection(users.indexOf(userSel).coerceAtLeast(0))
    }

    private fun setupTypeToggle(dlg: DialogFilterTransactionsBinding, currentF: TransactionFilter) {
        dlg.toggleType.isSelectionRequired = true
        val typeChecked = when (currentF.type) {
            TransactionEntity.TransactionType.INCOME -> R.id.btnTypeIncome
            TransactionEntity.TransactionType.EXPENSE -> R.id.btnTypeExpense
            else -> R.id.btnTypeAll
        }
        dlg.toggleType.check(typeChecked)
    }

    private fun setupDateFields(dlg: DialogFilterTransactionsBinding, currentF: TransactionFilter) {
        val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        pickedStart = currentF.startDate
        pickedEnd = currentF.endDate

        pickedStart?.let { dlg.etStartDate.setText(df.format(Date(it))) }
        pickedEnd?.let { dlg.etEndDate.setText(df.format(Date(it))) }

        fun pickDate(target: MaterialAutoCompleteTextView, onSet: (Long) -> Unit) {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .build()
            picker.addOnPositiveButtonClickListener { ts ->
                onSet(ts)
                target.setText(df.format(Date(ts)))
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }

        dlg.etStartDate.setOnClickListener { pickDate(dlg.etStartDate) { pickedStart = it } }
        dlg.etEndDate.setOnClickListener { pickDate(dlg.etEndDate) { pickedEnd = it } }

        setupDateFieldClearing(dlg)
    }

    private fun setupDateFieldClearing(dlg: DialogFilterTransactionsBinding) {
        dlg.etStartDateLayout.setEndIconOnClickListener {
            dlg.etStartDate.text?.clear()
            pickedStart = null
            dlg.etStartDateLayout.error = null
            dlg.etStartDateLayout.clearFocus()
        }

        dlg.etEndDateLayout.setEndIconOnClickListener {
            dlg.etEndDate.text?.clear()
            pickedEnd = null
            dlg.etEndDateLayout.error = null
            dlg.etEndDateLayout.clearFocus()
        }

        dlg.etStartDate.doAfterTextChanged { text ->
            if (text.isNullOrEmpty()) {
                pickedStart = null
                dlg.etStartDateLayout.error = null
            }
        }

        dlg.etEndDate.doAfterTextChanged { text ->
            if (text.isNullOrEmpty()) {
                pickedEnd = null
                dlg.etEndDateLayout.error = null
            }
        }
    }

    private fun setupAmountFields(dlg: DialogFilterTransactionsBinding, currentF: TransactionFilter) {
        val signedList = viewModel.transactions.value.map { t ->
            t.amount.toFloat()
        }
        val globalMin = (signedList.minOrNull() ?: 0f).roundTo(2)
        val globalMax = (signedList.maxOrNull() ?: 1f).roundTo(2)

        val curMin = (currentF.minAmount?.toFloat()?.coerceIn(globalMin, globalMax) ?: globalMin).roundTo(2)
        val curMax = (currentF.maxAmount?.toFloat()?.coerceIn(globalMin, globalMax) ?: globalMax).roundTo(2)

        setupAmountSlider(dlg, globalMin, globalMax, curMin, curMax)
        setupAmountInputs(dlg, globalMin, globalMax)
    }

    private fun setupAmountSlider(
        dlg: DialogFilterTransactionsBinding,
        globalMin: Float,
        globalMax: Float,
        curMin: Float,
        curMax: Float
    ) {
        dlg.sliderAmount.apply {
            valueFrom = globalMin
            valueTo = globalMax
            stepSize = 0f
            values = listOf(curMin, curMax)
        }
        dlg.etMinAmount.setText(curMin.toString())
        dlg.etMaxAmount.setText(curMax.toString())

        dlg.sliderAmount.addOnChangeListener { _, _, _ ->
            isSyncSliderDisabled = true
            dlg.etMinAmount.setText(dlg.sliderAmount.values[0].roundTo(2).toString())
            dlg.etMaxAmount.setText(dlg.sliderAmount.values[1].roundTo(2).toString())
            isSyncSliderDisabled = false
        }
    }

    private fun setupAmountInputs(
        dlg: DialogFilterTransactionsBinding,
        globalMin: Float,
        globalMax: Float
    ) {
        fun syncSlider() {
            if (isSyncSliderDisabled) return
            val low = (dlg.etMinAmount.text.toString().toFloatOrNull()?.coerceIn(globalMin, globalMax)
                ?: globalMin).roundTo(2)
            val high = (dlg.etMaxAmount.text.toString().toFloatOrNull()?.coerceIn(low, globalMax)
                ?: globalMax).roundTo(2)
            dlg.sliderAmount.values = listOf(low, high)
        }

        dlg.etMinAmount.doAfterTextChanged { syncSlider() }
        dlg.etMaxAmount.doAfterTextChanged { syncSlider() }
    }

    private fun createFilterDialog(dlg: DialogFilterTransactionsBinding): AlertDialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filters)
            .setView(dlg.root)
            .setPositiveButton(R.string.apply_filters, null)
            .setNegativeButton(R.string.clear_filters) { dialog, _ ->
                viewModel.applyFilter(TransactionFilter())
                dialog.dismiss()
            }
            .create()
    }

    private fun setupDialogButtons(dialog: AlertDialog, dlg: DialogFilterTransactionsBinding) {
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!validateDates(dlg) || !validateAmounts(dlg)) return@setOnClickListener

            val applied = TransactionFilter(
                category = dlg.spinnerCategory.selectedItem.toString()
                    .takeIf { it != getString(R.string.all) },
                type = when (dlg.toggleType.checkedButtonId) {
                    R.id.btnTypeIncome -> TransactionEntity.TransactionType.INCOME
                    R.id.btnTypeExpense -> TransactionEntity.TransactionType.EXPENSE
                    else -> null
                },
                userName = dlg.spinnerUser.selectedItem.toString()
                    .takeIf { it != getString(R.string.all) },
                startDate = pickedStart,
                endDate = pickedEnd,
                minAmount = dlg.etMinAmount.text.toString().toDoubleOrNull()?.roundTo(2),
                maxAmount = dlg.etMaxAmount.text.toString().toDoubleOrNull()?.roundTo(2)
            )
            viewModel.applyFilter(applied)
            dialog.dismiss()
        }
    }

    private fun validateDates(dlg: DialogFilterTransactionsBinding): Boolean {
        if (pickedStart != null && pickedEnd != null && pickedStart!! > pickedEnd!!) {
            dlg.etStartDateLayout.error = getString(R.string.error_date_order)
            Snackbar.make(dlg.root, R.string.error_date_order, Snackbar.LENGTH_SHORT).show()
            return false
        }
        dlg.etStartDateLayout.error = null
        return true
    }

    private fun validateAmounts(dlg: DialogFilterTransactionsBinding): Boolean {
        val selMin = dlg.etMinAmount.text.toString().toDoubleOrNull()?.roundTo(2)
        val selMax = dlg.etMaxAmount.text.toString().toDoubleOrNull()?.roundTo(2)
        if (selMin != null && selMax != null && selMin > selMax) {
            dlg.etMinAmountLayout.error = getString(R.string.error_min_max_amount)
            Snackbar.make(dlg.root, R.string.error_min_max_amount, Snackbar.LENGTH_SHORT).show()
            return false
        }
        dlg.etMinAmountLayout.error = null
        return true
    }

    private fun openAddTransactionDialog() {
        val dialog = AddTransactionDialogFragment.newInstance(
            projectId = args.projectId,
            currentRole = projectData?.currentRole?.name.orEmpty()
        )
        dialog.setOnTransactionAdded { viewModel.addTransaction(args.projectId, it.toEntity()) }
        dialog.show(childFragmentManager, "AddTransaction")
    }

    private fun onTransactionClicked(transaction: TransactionEntity) {
        val dialog = AddTransactionDialogFragment.newInstance(
            projectId = args.projectId,
            currentRole = if (projectData?.project?.status == ProjectStatus.DELETED) {
                ParticipantRole.VIEWER.name
            } else {
                projectData?.currentRole?.name.orEmpty()
            },
            transactionId = transaction.id
        )

        dialog.setOnTransactionUpdated {
            viewModel.updateTransaction(args.projectId, it.toEntity())
        }
        dialog.setOnTransactionDeleted {
            viewModel.deleteTransaction(args.projectId, transaction.id)
        }
        dialog.show(childFragmentManager, "TransactionDetails")
    }

    private fun onTransactionDelete(transaction: TransactionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_transaction))
            .setMessage(getString(R.string.delete_transaction_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTransaction(args.projectId, transaction.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun onMenuItemClicked(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menuEdit -> {
                navigateToEdit(); true
            }

            R.id.menuDelete -> {
                confirmDelete(); true
            }

            R.id.menuArchiveProject -> {
                changeStatus(ProjectStatus.ARCHIVED); true
            }

            R.id.menuUnarchiveProject -> {
                changeStatus(ProjectStatus.ACTIVE); true
            }

            R.id.menuParticipants -> {
                navigateToParticipants(); true
            }

            R.id.menuAnalytics -> {
                navigateToAnalytics(); true
            }

            R.id.menuProjectNotifications -> {
                navigateToProjectNotificaitons(); true
            }

            else -> false
        }

    private fun changeStatus(newStatus: ProjectStatus) {
        projectData?.project?.copy(status = newStatus)?.let {
            viewModel.updateProject(it)
        }
        Snackbar.make(
            binding.root,
            getString(
                if (newStatus == ProjectStatus.ARCHIVED)
                    R.string.project_archived
                else
                    R.string.project_unarchived
            ),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun refreshData() {
        viewModel.syncProjectDetails(args.projectId)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun navigateToEdit() {
        val bottomSheet = EditProjectDialogFragment(args.projectId)
        bottomSheet.setOnProjectEdited {
            viewModel.syncProjectDetails(args.projectId)
        }
        bottomSheet.show(childFragmentManager, EditProjectDialogFragment.TAG)
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_project)
            .setMessage(R.string.delete_project_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteProject(args.projectId)
                viewModel.syncProjects()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToParticipants() =
        findNavController().navigate(
            ProjectDetailsFragmentDirections.actionDetailsToParticipants(
                projectId = args.projectId,
                userRole = projectData?.currentRole?.name.orEmpty(),
                projectStatus = projectData?.project?.status?.type ?: ProjectStatus.ACTIVE.type
            )
        )

    private fun navigateToAnalytics() =
        findNavController().navigate(
            ProjectDetailsFragmentDirections.actionDetailsToAnalytics(args.projectId)
        )

    private fun navigateToProjectNotificaitons() {
        val bottomSheet = ProjectNotificationsBottomSheet(
            userRole = projectData?.currentRole ?: ParticipantRole.VIEWER,
            projectId = args.projectId
        )
        bottomSheet.show(childFragmentManager, ProjectNotificationsBottomSheet.TAG)
    }

    companion object {
        private const val KEY_TEXT_EXPANDED = "KEY_TEXT_EXPANDED"
        private const val KEY_BUDGET_EXPANDED = "KEY_BUDGET_EXPANDED"
        private const val MAX_COLLAPSED_LINES = 3
        private const val MIN_DESC_LENGTH = 300
    }
}
