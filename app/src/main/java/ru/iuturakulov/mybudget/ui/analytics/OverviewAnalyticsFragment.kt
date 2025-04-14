package ru.iuturakulov.mybudget.ui.analytics

import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.CategoryDistributionDto
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.PeriodDistributionDto
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

    private fun updateCategoryChart(categories: List<CategoryDistributionDto>) {
        binding.categoryPieChart.apply {
            if (categories.isNotEmpty()) {
                // Сортируем по убыванию суммы для лучшего отображения
                val sortedCategories = categories.sortedByDescending { it.totalAmount }

                val entries = sortedCategories.map {
                    PieEntry(it.totalAmount.toFloat(), it.category)
                }

                val dataSet = PieDataSet(entries, "").apply {
                    colors = getCategoryColors(categories.size)
                    valueTextSize = 12f
                    valueTextColor = Color.BLACK
                    valueFormatter = PercentFormatter(binding.categoryPieChart)
                    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                    valueLinePart1Length = 0.4f
                    valueLinePart2Length = 0.4f
                }

                this.data = PieData(dataSet).apply {
                    setValueTextSize(12f)
                }

                description.isEnabled = false
                legend.isEnabled = true
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.textSize = 12f

                setUsePercentValues(true)
                setEntryLabelColor(Color.BLACK)
                setEntryLabelTextSize(12f)
                setDrawEntryLabels(false)
                setHoleColor(Color.TRANSPARENT)
                setTransparentCircleAlpha(0)
                animateY(1000, Easing.EaseInOutQuad)
                invalidate()
            } else {
                clear()
                setNoDataText("Нет данных по категориям")
                setNoDataTextColor(ContextCompat.getColor(context, R.color.chart_no_data_text))
            }
        }
    }

    private fun updatePeriodChart(periods: List<PeriodDistributionDto>) {
        binding.periodBarChart.apply {
            if (periods.isNotEmpty()) {
                val entries = periods.mapIndexed { index, period ->
                    BarEntry(index.toFloat(), period.totalAmount.toFloat())
                }

                val dataSet = BarDataSet(entries, "").apply {
                    color = ContextCompat.getColor(context, R.color.chart_color_green)
                    valueTextSize = 12f
                    valueFormatter = LargeValueFormatter()
                    setDrawValues(true)
                }

                this.data = BarData(dataSet).apply {
                    barWidth = 0.5f
                    setValueTextSize(12f)
                }

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(periods.map { it.period })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    textSize = 12f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    granularity = if (periods.maxOf { it.totalAmount } > 10000) 2000f else 500f
                    axisMinimum = 0f
                    textSize = 12f
                }

                axisRight.isEnabled = false
                description.isEnabled = false
                legend.isEnabled = false
                setFitBars(true)
                animateY(1000, Easing.EaseInOutQuad)
                invalidate()
            } else {
                clear()
                setNoDataText("Нет данных по периодам")
                setNoDataTextColor(ContextCompat.getColor(context, R.color.chart_no_data_text))
            }
        }
    }

    private fun updateCharts(data: OverviewAnalyticsDto) {
        try {
            updatePeriodChart(data.periodDistribution)
            updateCategoryChart(data.categoryDistribution)

            // Обновление BarChart для сравнения проектов
            val projectEntries = data.projectComparison.mapIndexed { index, project ->
                BarEntry(index.toFloat(), project.totalSpent.toFloat())
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

    private fun getCategoryColors(count: Int): List<Int> {
        return when {
            count <= 5 -> listOf(
                ContextCompat.getColor(requireContext(), R.color.chart_color_primary),
                ContextCompat.getColor(requireContext(), R.color.chart_color_green),
                ContextCompat.getColor(requireContext(), R.color.chart_color_orange),
                ContextCompat.getColor(requireContext(), R.color.chart_color_red),
                ContextCompat.getColor(requireContext(), R.color.chart_color_purple),
                ContextCompat.getColor(requireContext(), R.color.chart_color_secondary),
                ContextCompat.getColor(requireContext(), R.color.chart_color_accent)
            )
            else -> ColorTemplate.MATERIAL_COLORS.toList()
        }
    }
}
