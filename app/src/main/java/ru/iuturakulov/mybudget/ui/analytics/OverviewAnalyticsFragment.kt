package ru.iuturakulov.mybudget.ui.analytics

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
import com.google.android.material.snackbar.Snackbar
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
            viewModel.loadOverviewAnalytics()
        }
    }

    private fun setupCharts() {
        // Настройка PieChart для категорий
        binding.categoryPieChart.apply {
            description.isEnabled = false
            isRotationEnabled = true
            legend.isEnabled = false
            setNoDataText("Нет данных по категориям")
            setNoDataTextColor(R.color.orange)
        }

        // Настройка BarChart для периодов
        binding.periodBarChart.apply {
            description.isEnabled = false
            setFitBars(true)
            legend.isEnabled = false
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
            }
            setNoDataText("Нет данных по периодам")
            setNoDataTextColor(R.color.orange)
        }

        // Настройка BarChart для сравнения проектов
        binding.projectComparisonChart.apply {
            description.isEnabled = false
            setFitBars(true)
            legend.isEnabled = false
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
            }
            setNoDataText("Нет данных по проектам")
            setNoDataTextColor(R.color.orange)
        }
    }

    private fun observeOverviewAnalytics() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.overviewAnalytics.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showLoading()
                    }
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        if (state.data != null) {
                            showContent()
                            updateCharts(state.data)
                        } else {
                            showFallback("Нет данных аналитики")
                        }
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        showError(state.message)
                        showFallback("Ошибка загрузки данных")
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun showContent() {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showFallback(message: String) {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isRefreshing = false
        // TODO: Можно добавить отдельные fallback-вью или просто уведомить пользователя
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isRefreshing = false
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Повторить") {
                viewModel.loadOverviewAnalytics()
            }
            .show()
    }

    private fun updateCharts(data: OverviewAnalyticsDto) {
        try {
            // Обновление PieChart для категорий
            val pieEntries = data.categoryDistribution.map {
                PieEntry(it.amount.toFloat(), it.category)
            }
            if (pieEntries.isNotEmpty()) {
                val pieDataSet = PieDataSet(pieEntries, "Категории").apply {
                    colors = ColorTemplate.MATERIAL_COLORS.toList()
                    valueTextSize = 12f
                    valueFormatter = PercentFormatter(binding.categoryPieChart)
                }
                binding.categoryPieChart.apply {
                    this.data = PieData(pieDataSet)
                    setUsePercentValues(true)
                    animateY(1000)
                    invalidate()
                }
            } else {
                binding.categoryPieChart.clear()
            }

            // Обновление BarChart для периодов
            val periodEntries = data.periodDistribution.mapIndexed { index, period ->
                BarEntry(index.toFloat(), period.amount.toFloat())
            }
            if (periodEntries.isNotEmpty()) {
                val periodDataSet = BarDataSet(periodEntries, "Периоды").apply {
                    colors = ColorTemplate.COLORFUL_COLORS.toList()
                    valueTextSize = 10f
                }
                binding.periodBarChart.apply {
                    this.data = BarData(periodDataSet)
                    xAxis.valueFormatter =
                        IndexAxisValueFormatter(data.periodDistribution.map { it.period })
                    animateY(1000)
                    invalidate()
                }
            } else {
                binding.periodBarChart.clear()
            }

            // Обновление BarChart для сравнения проектов
            val projectEntries = data.projectComparison.mapIndexed { index, project ->
                BarEntry(index.toFloat(), project.amount.toFloat())
            }
            if (projectEntries.isNotEmpty()) {
                val projectDataSet = BarDataSet(projectEntries, "Проекты").apply {
                    colors = ColorTemplate.LIBERTY_COLORS.toList()
                    valueTextSize = 10f
                }
                binding.projectComparisonChart.apply {
                    this.data = BarData(projectDataSet)
                    xAxis.valueFormatter =
                        IndexAxisValueFormatter(data.projectComparison.map { it.projectName })
                    animateY(1000)
                    invalidate()
                }
            } else {
                binding.projectComparisonChart.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Ошибка отображения данных")
        }
    }
}
