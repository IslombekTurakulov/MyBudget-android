package ru.iuturakulov.mybudget.ui.analytics

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.databinding.FragmentOverviewAnalyticsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class OverviewAnalyticsFragment :
    BaseFragment<FragmentOverviewAnalyticsBinding>(R.layout.fragment_overview_analytics) {

    private val viewModel: AnalyticsViewModel by viewModels()

    override fun getViewBinding(view: View): FragmentOverviewAnalyticsBinding {
        return FragmentOverviewAnalyticsBinding.bind(view)
    }

    override fun setupViews() {
        setupSwipeRefresh()
        setupCharts()
    }

    override fun setupObservers() {
        observeOverviewAnalytics()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadOverviewAnalytics() // Загружаем аналитику
        }
    }

    private fun setupCharts() {
        binding.categoryPieChart.apply {
            description.isEnabled = false
            isRotationEnabled = true
            legend.isEnabled = false
        }

        binding.periodBarChart.apply {
            description.isEnabled = false
            setFitBars(true)
            legend.isEnabled = false
        }

        binding.projectComparisonChart.apply {
            description.isEnabled = false
            setFitBars(true)
            legend.isEnabled = false
        }
    }

    private fun observeOverviewAnalytics() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.overviewAnalytics.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        showContent()
                        state.data?.let { updateCharts(it) }
                    }

                    is UiState.Error -> showError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
    }

    private fun showContent() {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isRefreshing = false
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun updateCharts(data: OverviewAnalyticsDto) {
        // Категории (PieChart)
        val pieEntries = data.categoryDistribution.map {
            PieEntry(it.amount.toFloat(), it.category)
        }
        val pieDataSet = PieDataSet(pieEntries, "Категории").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueFormatter = PercentFormatter()
        }
        binding.categoryPieChart.apply {
            this.data = PieData(pieDataSet)
            setUsePercentValues(true)
            invalidate()
        }

        // Периоды (BarChart)
        val periodEntries = data.periodDistribution.mapIndexed { index, period ->
            BarEntry(index.toFloat(), period.amount.toFloat())
        }
        val periodDataSet = BarDataSet(periodEntries, "Периоды").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            valueTextSize = 10f
        }
        binding.periodBarChart.apply {
            this.data = BarData(periodDataSet)
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(data.periodDistribution.map { it.period })
            }
            invalidate()
        }

        // Сравнение проектов (BarChart)
        val projectEntries = data.projectComparison.mapIndexed { index, project ->
            BarEntry(index.toFloat(), project.amount.toFloat())
        }
        val projectDataSet = BarDataSet(projectEntries, "Проекты").apply {
            colors = ColorTemplate.LIBERTY_COLORS.toList()
            valueTextSize = 10f
        }
        binding.projectComparisonChart.apply {
            this.data = BarData(projectDataSet)
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter =
                    IndexAxisValueFormatter(data.projectComparison.map { it.projectName })
            }
            invalidate()
        }
    }
}
