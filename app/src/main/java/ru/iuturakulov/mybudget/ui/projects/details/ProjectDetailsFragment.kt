package ru.iuturakulov.mybudget.ui.projects.details

import android.app.AlertDialog
import android.os.Bundle
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
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
    private lateinit var transactionAdapter: TransactionAdapter
    private var projectData: ProjectWithTransactions? = null

    override fun getViewBinding(view: View) =
        FragmentProjectDetailsBinding.bind(view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            isTextExpanded = it.getBoolean(KEY_TEXT_EXPANDED, false)
        }
        viewModel.loadProjectDetails(args.projectId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_TEXT_EXPANDED, isTextExpanded)
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
        updateDescription()

        btnFilterTransactions.setOnClickListener { showFilterDialog() }
        fabAddTransaction.setOnClickListener { openAddTransactionDialog() }

        rvTransactions.addOnScrollListener(scrollListener)
    }

    private fun setupRecyclerView() = binding.rvTransactions.apply {
        transactionAdapter = TransactionAdapter(::onTransactionClicked)
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

        tvProjectBudget.text =
            getString(R.string.project_budget, data.project.budgetLimit)
        tvProjectSpent.text =
            getString(R.string.project_spent, data.project.amountSpent)
        tvRemainingBudget.text = getString(
            R.string.project_remaining,
            data.project.budgetLimit - data.project.amountSpent
        )

        data.project.description?.takeIf(String::isNotBlank)?.let { desc ->
            projectDescriptionCard.isVisible = true
            tvProjectDescription.text = desc
            btnToggleDescription.isVisible = desc.length >= MIN_DESC_LENGTH
        } ?: run {
            projectDescriptionCard.isGone = true
        }
    }

    private fun processUserRole(role: ParticipantRole?, status: ProjectStatus) = binding.apply {
        val isViewer = role == ParticipantRole.VIEWER
        val isProjectDeleted = status == ProjectStatus.DELETED
        toolbar.menu.findItem(R.id.menuEdit).isVisible = !isViewer && !isProjectDeleted
        toolbar.menu.setGroupVisible(
            R.id.group_editor,
            role == ParticipantRole.OWNER && !isProjectDeleted
        )
        fabAddTransaction.isVisible = role != ParticipantRole.VIEWER && !isProjectDeleted
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
            emptyTransactionsLayout.isVisible = true
            rvTransactions.isGone = true
        } else {
            emptyTransactionsLayout.isGone = true
            emptyTransactionsLayout.isGone = true
            rvTransactions.isVisible = true
            transactionAdapter.submitList(list)
            rvTransactions.scheduleLayoutAnimation()
        }
    }

    private fun toggleDescription() {
        isTextExpanded = !isTextExpanded
        TransitionManager.beginDelayedTransition(binding.projectDescriptionCard)
        updateDescription()
    }

    private fun updateDescription() = binding.apply {
        tvProjectDescription.maxLines =
            if (isTextExpanded) Int.MAX_VALUE else MAX_COLLAPSED_LINES
        btnToggleDescription.apply {
            text = getString(
                if (isTextExpanded) R.string.show_less
                else R.string.show_more
            )
            setIconResource(
                if (isTextExpanded) R.drawable.baseline_expand_less_24
                else R.drawable.baseline_expand_more_24
            )
        }
    }

    private fun showFilterDialog() {
        val dlg = DialogFilterTransactionsBinding.inflate(layoutInflater)
        val currentF = viewModel.currentFilter.value.copy()

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

        val users = listOf(getString(R.string.all)) + viewModel.transactions.value
            .mapNotNull { it.userName.takeIf(String::isNotBlank) }
            .distinct()
        dlg.spinnerUser.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            users
        )
        val userSel = currentF.userName ?: getString(R.string.all)
        dlg.spinnerUser.setSelection(users.indexOf(userSel).coerceAtLeast(0))

        dlg.toggleType.isSelectionRequired = true
        val typeChecked = when (currentF.type) {
            TransactionEntity.TransactionType.INCOME -> R.id.btnTypeIncome
            TransactionEntity.TransactionType.EXPENSE -> R.id.btnTypeExpense
            else -> R.id.btnTypeAll
        }
        dlg.toggleType.check(typeChecked)

        val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        var pickedStart = currentF.startDate
        var pickedEnd = currentF.endDate
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

        val signedList = viewModel.transactions.value.map { t ->
            when (TransactionEntity.TransactionType.fromString(t.type)) {
                TransactionEntity.TransactionType.INCOME -> t.amount.toFloat()
                TransactionEntity.TransactionType.EXPENSE -> -t.amount.toFloat()
            }
        }
        val globalMin = signedList.minOrNull() ?: -1000f
        val globalMax = signedList.maxOrNull() ?: 1000f

        val curMin = currentF.minAmount?.toFloat()?.coerceIn(globalMin, globalMax) ?: globalMin
        val curMax = currentF.maxAmount?.toFloat()?.coerceIn(globalMin, globalMax) ?: globalMax

        var isSyncSliderDisabled = false

        dlg.sliderAmount.apply {
            valueFrom = globalMin
            valueTo = globalMax
            stepSize = 1f
            values = listOf(curMin, curMax)
        }
        dlg.etMinAmount.setText(curMin.toString())
        dlg.etMaxAmount.setText(curMax.toString())

        dlg.sliderAmount.addOnChangeListener { _, _, _ ->
            isSyncSliderDisabled = true
            dlg.etMinAmount.setText(dlg.sliderAmount.values[0].toString())
            dlg.etMaxAmount.setText(dlg.sliderAmount.values[1].toString())
            isSyncSliderDisabled = false
        }
        fun syncSlider() {
            if (isSyncSliderDisabled) return
            val low =
                dlg.etMinAmount.text.toString().toFloatOrNull()?.coerceIn(globalMin, globalMax)
                    ?: globalMin
            val high = dlg.etMaxAmount.text.toString().toFloatOrNull()?.coerceIn(low, globalMax)
                ?: globalMax
            dlg.sliderAmount.values = listOf(low, high)
        }

        dlg.etMinAmount.doAfterTextChanged { syncSlider() }
        dlg.etMaxAmount.doAfterTextChanged { syncSlider() }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filters)
            .setView(dlg.root)
            .setPositiveButton(R.string.apply_filters, null)
            .setNegativeButton(R.string.clear_filters) { dialog, _ ->
                viewModel.applyFilter(TransactionFilter())
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (pickedStart != null && pickedEnd != null && pickedStart!! > pickedEnd!!) {
                dlg.etStartDateLayout.error = getString(R.string.error_date_order)
                Snackbar.make(dlg.root, R.string.error_date_order, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                dlg.etStartDateLayout.error = null
            }

            val selMin = dlg.etMinAmount.text.toString().toDoubleOrNull()
            val selMax = dlg.etMaxAmount.text.toString().toDoubleOrNull()
            if (selMin != null && selMax != null && selMin > selMax) {
                dlg.etMinAmountLayout.error = getString(R.string.error_min_max_amount)
                Snackbar.make(dlg.root, R.string.error_min_max_amount, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                dlg.etMinAmountLayout.error = null
            }

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
                minAmount = selMin,
                maxAmount = selMax
            )
            viewModel.applyFilter(applied)
            dialog.dismiss()
        }
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
            currentRole = projectData?.currentRole?.name.orEmpty(),
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
        bottomSheet.show(childFragmentManager, ProjectFilterBottomSheet.TAG)
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

    companion object {
        private const val KEY_TEXT_EXPANDED = "KEY_TEXT_EXPANDED"
        private const val MAX_COLLAPSED_LINES = 3
        private const val MIN_DESC_LENGTH = 300
    }
}
