package ru.iuturakulov.mybudget.ui.analytics

import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.ProjectAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.TaskComparisonDto
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

        // Настройка BarChart для сравнения задач
        binding.taskComparisonChart.apply {
            description.isEnabled = false
            setFitBars(true)
            legend.isEnabled = false
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
            }
            setNoDataText("Нет данных по задачам")
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
            // Обновление PieChart для категорий
            val pieEntries = data.categoryDistribution.map {
                PieEntry(it.amount.toFloat(), it.category)
            }
            if (pieEntries.isNotEmpty()) {
                val pieDataSet = PieDataSet(pieEntries, "Категории").apply {
                    colors = ColorTemplate.MATERIAL_COLORS.toList()
                    valueTextSize = 12f
                }
                binding.categoryPieChart.apply {
                    this.data = PieData(pieDataSet)
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
                    valueTextSize = 12f
                }
                binding.periodBarChart.apply {
                    this.data = BarData(periodDataSet)
                    xAxis.valueFormatter = IndexAxisValueFormatter(data.periodDistribution.map { it.period })
                    animateY(1000)
                    invalidate()
                }
            } else {
                binding.periodBarChart.clear()
            }

            // Обновление BarChart для сравнения задач
            // updateTaskComparisonChart(data.taskComparison)
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Ошибка отображения данных")
        }
    }

    private fun updateTaskComparisonChart(taskComparison: List<TaskComparisonDto>) {
        // Создаем списки для выполненных и невыполненных задач
        val completedEntries = mutableListOf<BarEntry>()
        val pendingEntries = mutableListOf<BarEntry>()

        taskComparison.forEachIndexed { index, task ->
            completedEntries.add(BarEntry(index.toFloat(), task.completedTasks.toFloat()))
            pendingEntries.add(
                BarEntry(
                    index.toFloat(),
                    (task.totalTasks - task.completedTasks).toFloat()
                )
            )
        }

        if (completedEntries.isNotEmpty() && pendingEntries.isNotEmpty()) {
            val completedDataSet = BarDataSet(completedEntries, "Выполнено").apply {
                color = Color.GREEN
                valueTextSize = 12f
            }
            val pendingDataSet = BarDataSet(pendingEntries, "Ожидает").apply {
                color = Color.RED
                valueTextSize = 12f
            }

            val barData = BarData(completedDataSet, pendingDataSet).apply {
                barWidth = 0.4f
            }

            // Формирование подписей для оси X
            val taskLabels = taskComparison.mapIndexed { index, _ -> "Task ${index + 1}" }

            binding.taskComparisonChart.apply {
                data = barData
                xAxis.valueFormatter = IndexAxisValueFormatter(taskLabels)
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisLeft.axisMinimum = 0f
                axisRight.isEnabled = false
                legend.isEnabled = true
                groupBars(0f, 0.2f, 0.05f)
                animateY(1000)
                invalidate()
            }
        } else {
            binding.taskComparisonChart.clear()
        }
    }
}
