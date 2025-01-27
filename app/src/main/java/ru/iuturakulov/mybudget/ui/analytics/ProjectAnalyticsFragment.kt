package ru.iuturakulov.mybudget.ui.analytics

import android.graphics.Color
import android.view.View
import android.widget.Toast
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

        binding.taskComparisonChart.apply {
            description.isEnabled = false
            setFitBars(true)
            legend.isEnabled = false
        }
    }

    private fun observeAnalyticsData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
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
        binding.swipeRefreshLayout.isVisible = false
    }

    private fun showContent() {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isVisible = true
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        binding.swipeRefreshLayout.isVisible = true
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun updateCharts(data: ProjectAnalyticsDto) {
        // Обновление PieChart
        val pieEntries = data.categoryDistribution.map {
            PieEntry(it.amount.toFloat(), it.category)
        }
        val pieDataSet = PieDataSet(pieEntries, "Категории").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
        }
        binding.categoryPieChart.data = PieData(pieDataSet)
        binding.categoryPieChart.invalidate()

        // Обновление Period BarChart
        val periodEntries = data.periodDistribution.mapIndexed { index, period ->
            BarEntry(index.toFloat(), period.amount.toFloat())
        }
        val periodDataSet = BarDataSet(periodEntries, "Периоды").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
        }
        binding.periodBarChart.data = BarData(periodDataSet)
        binding.periodBarChart.invalidate()

        // Обновление Task Comparison Chart
        updateTaskComparisonChart(data.taskComparison)
    }

    private fun updateTaskComparisonChart(taskComparison: List<TaskComparisonDto>) {
        // Создаем список для выполненных и невыполненных задач
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

        // DataSet для выполненных задач
        val completedDataSet = BarDataSet(completedEntries, "Выполнено").apply {
            color = Color.GREEN
            valueTextSize = 12f
        }

        // DataSet для невыполненных задач
        val pendingDataSet = BarDataSet(pendingEntries, "Ожидает").apply {
            color = Color.RED
            valueTextSize = 12f
        }

        // Установка данных в BarData
        val barData = BarData(completedDataSet, pendingDataSet).apply {
            barWidth = 0.4f // Ширина столбцов
        }

        val taskLabels = taskComparison.mapIndexed { index, _ -> "Task ${index + 1}" }


        // Настройка графика
        binding.taskComparisonChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            xAxis.apply {
                granularity = 1f
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(taskLabels)
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = true
            groupBars(0f, 0.2f, 0.05f) // Группировка столбцов
            invalidate()
        }
    }

}
