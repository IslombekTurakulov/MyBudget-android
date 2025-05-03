package ru.iuturakulov.mybudget.ui.analytics

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.CurrencyFormatter
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsFilter
import ru.iuturakulov.mybudget.data.remote.dto.Granularity
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.OverviewCategoryStats
import ru.iuturakulov.mybudget.data.remote.dto.OverviewPeriodStats
import ru.iuturakulov.mybudget.databinding.FragmentBaseAnalyticsBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class OverviewAnalyticsFragment : BaseAnalyticsFragment<
        FragmentBaseAnalyticsBinding,
        OverviewAnalyticsDto,
        OverviewCategoryStats,
        OverviewPeriodStats
        >(
    R.layout.fragment_base_analytics,
    FragmentBaseAnalyticsBinding::bind
) {

    override lateinit var viewModel: AnalyticsViewModel
    override val analyticsStateFlow: Flow<UiState<OverviewAnalyticsDto>>
        get() = viewModel.filteredOverviewAnalytics

    override val initialAnalyticsStateFlow: Flow<UiState<OverviewAnalyticsDto>>
        get() = viewModel.initialOverviewAnalytics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[AnalyticsViewModel::class.java]
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.overview_analytics)
        binding.toolbar.navigationIcon = null
        binding.projectCard.visibility = View.VISIBLE
        startAnalytics()
    }

    override fun startAnalytics() {
        viewModel.startOverviewProjectAnalytics()
    }

    override fun bindHeaderChips(dto: OverviewAnalyticsDto) {
        val initialCategories = dto.categoryDistribution.map { category ->
            if (category.category == "Без категории") {
                getString(R.string.no_category)
            } else {
                category.category
            }
        }
        val chipGroup =
            binding.root.findViewById<ChipGroup>(R.id.chipGroupCategories)
        initCategoryChips(
            chipGroup = chipGroup,
            categories = initialCategories,
            preselect = viewModel.appliedFilter.value.categories
                ?: initialCategories,
            clearCharts = {
                binding.categoryPieChart.highlightValues(null)
                binding.periodBarChart.highlightValues(null)
                binding.projectComparisonChart?.highlightValues(null)
            }
        )  { selectedCategories ->
            viewModel.applyFilter(
                viewModel.appliedFilter.value.copy(categories = selectedCategories)
            )
        }
    }

    override fun bindHeader(dto: OverviewAnalyticsDto) {
        val periods = dto.periodDistribution.map { it.period }
        val first = periods.firstOrNull().orEmpty()
        val last = periods.lastOrNull().orEmpty()

        binding.tvSubtitle.text = getString(
            R.string.analytics_period_subtitle,
            first,
            last
        )
        binding.tvTotal.text = getString(
            R.string.total_analytics_amount_format,
            CurrencyFormatter.format(dto.totalAmount)
        )

        val categoryStats = dto.categoryDistribution.map { category ->
            category.copy(
                category = if (category.category == "Без категории") {
                    getString(R.string.no_category)
                } else {
                    category.category
                }
            )
        }
        updateCategoryChart(
            chart = binding.categoryPieChart,
            items = categoryStats
        ) {
            PieEntry(it.amount.toFloat(), it.category)
        }
        updatePeriodChart(
            chart = binding.periodBarChart,
            items = dto.periodDistribution,
            toEntry = {
                BarEntry(
                    dto.periodDistribution.indexOf(it).toFloat(),
                    it.amount.toFloat()
                )
            },
            labels = dto.periodDistribution.map { it.period }
        )
        // сравнение проектов
        updatePeriodChart(
            chart = binding.projectComparisonChart,
            items = dto.projectComparison,
            toEntry = {
                BarEntry(
                    dto.projectComparison.indexOf(it).toFloat(),
                    it.amount.toFloat()
                )
            },
            labels = dto.projectComparison.map { it.projectName }
        )

        setupOverviewInteractions(
            overviewCategoryStats = categoryStats,
            overviewPeriodStats = dto.periodDistribution,
        )
    }

    override fun openFilterDialog() {
        val current = viewModel.appliedFilter.value
        val state = viewModel.initialOverviewAnalytics.value
        if (state !is UiState.Success) return
        val data = state.data

        val dlgView = layoutInflater.inflate(R.layout.dialog_filter_analytics, null)
        val btnFrom = dlgView.findViewById<MaterialButton>(R.id.btnFromDate)
        val btnTo = dlgView.findViewById<MaterialButton>(R.id.btnToDate)
        val chipGroup = dlgView.findViewById<ChipGroup>(R.id.chipGroupCats)
        val acGran = dlgView.findViewById<AutoCompleteTextView>(R.id.acGranularity)

        fun fmt(ts: Long?) = ts?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(it))
        } ?: getString(R.string.not_selected)

        var selFrom = current.fromDate
        var selTo = current.toDate

        btnFrom.text = getString(R.string.from_date_label, fmt(selFrom))
        btnFrom.setOnClickListener {
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_start_period))
                .setSelection(selFrom ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build().apply {
                    addOnPositiveButtonClickListener {
                        selFrom = it
                        btnFrom.text = getString(R.string.from_date_label, fmt(selFrom))
                    }
                }.show(parentFragmentManager, "DATE_FROM")
        }

        btnTo.text = getString(R.string.to_date_label, fmt(selTo))
        btnTo.setOnClickListener {
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_end_period))
                .setSelection(selTo ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build().apply {
                    addOnPositiveButtonClickListener {
                        selTo = it
                        btnTo.text = getString(R.string.to_date_label, fmt(selTo))
                    }
                }.show(parentFragmentManager, "DATE_TO")
        }

        chipGroup.removeAllViews()
        val allCats = data?.categoryDistribution.orEmpty().map { categoryStats ->
            if (categoryStats.category == "Без категории") {
                getString(R.string.no_category)
            } else {
                categoryStats.category
            }
        }
        val preselect = current.categories ?: allCats
        allCats.forEach { c ->
            chipGroup.addView(
                Chip(
                    requireContext(), null,
                    com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter
                ).apply {
                    id = View.generateViewId()
                    text = c
                    isCheckable = true
                    isClickable = true
                    isChecked = preselect.contains(c)
                }
            )
        }

        val granList = Granularity.entries.map { it.name }
        acGran.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                granList
            )
        )
        acGran.setText(current.granularity.name, false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.analytics_filters_title)
            .setView(dlgView)
            .setPositiveButton(R.string.apply) { _, _ ->
                val selCats = chipGroup.checkedChipIds.map { id ->
                    dlgView.findViewById<Chip>(id).text.toString()
                }
                val cats = selCats.takeIf { it.size != allCats.size }
                val gran = Granularity.fromFilter(acGran.text.toString())
                    ?: Granularity.MONTH

                viewModel.applyFilter(
                    AnalyticsFilter(
                        fromDate = selFrom,
                        toDate = selTo,
                        categories = cats,
                        granularity = gran
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}