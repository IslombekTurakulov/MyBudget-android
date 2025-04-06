package ru.iuturakulov.mybudget.ui.analytics

import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import ru.iuturakulov.mybudget.data.remote.dto.PeriodDistributionDto
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import ru.iuturakulov.mybudget.databinding.FragmentProjectAnalyticsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ProjectAnalyticsFragment :
    BaseFragment<FragmentProjectAnalyticsBinding>(R.layout.fragment_project_analytics) {

    private val viewModel: ProjectAnalyticsViewModel by viewModels()
    private val args: ProjectAnalyticsFragmentArgs by navArgs()

    override fun getViewBinding(view: View): FragmentProjectAnalyticsBinding {
        return FragmentProjectAnalyticsBinding.bind(view)
    }

    override fun setupViews() {
        setupToolbar()
        setupSwipeRefresh()
        setupCharts()
        viewModel.loadProjectAnalytics(args.projectId)
    }

    override fun setupObservers() {
        observeAnalyticsData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadProjectAnalytics(args.projectId)
            // Сброс состояния будет выполнен после получения данных
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
    }

    private fun observeAnalyticsData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        showContent()
                        binding.swipeRefreshLayout.isRefreshing = false
                        state.data?.let { updateCharts(it) } ?: showFallback("Нет данных аналитики")
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        showError(state.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.swipeRefreshLayout.isVisible = false
    }

    private fun showContent() {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isVisible = true
    }

    private fun showFallback(message: String) {
        showContent()
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Повторить") {
                viewModel.loadProjectAnalytics(args.projectId)
            }
            .show()
    }

    private fun updateCharts(data: ProjectAnalyticsDto) {
        try {
            updateCategoryChart(data.categoryDistribution)
            updatePeriodChart(data.periodDistribution)
            updateTotalAmount(data.totalAmount)
        } catch (e: Exception) {
            showError("Ошибка отображения данных")
        }
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

    private fun updateTotalAmount(total: Double) {
        binding.tvTotalAmount.text = getString(R.string.total_amount_format, total)
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
