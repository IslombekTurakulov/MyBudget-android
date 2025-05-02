package ru.iuturakulov.mybudget.ui.analytics

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsExportFormat
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsFilter
import ru.iuturakulov.mybudget.data.remote.dto.Granularity
import ru.iuturakulov.mybudget.data.remote.dto.OverviewAnalyticsDto
import ru.iuturakulov.mybudget.data.remote.dto.OverviewCategoryStats
import ru.iuturakulov.mybudget.data.remote.dto.OverviewPeriodStats
import ru.iuturakulov.mybudget.data.remote.dto.ProjectComparisonStats
import ru.iuturakulov.mybudget.databinding.FragmentOverviewAnalyticsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class OverviewAnalyticsFragment :
    BaseFragment<FragmentOverviewAnalyticsBinding>(R.layout.fragment_overview_analytics) {

    private val viewModel: AnalyticsViewModel by viewModels()

    private var progressDialog: AlertDialog? = null
    private var tempFile: File? = null

    override fun getViewBinding(view: View): FragmentOverviewAnalyticsBinding =
        FragmentOverviewAnalyticsBinding.bind(view)

    override fun setupViews() {
        setupToolbar()
        setupSwipeRefresh()
        setupCharts()
    }

    override fun setupObservers() {
//        observeFilters()
        observeOverviewAnalytics()
        collectExportState()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.startOverviewProjectAnalytics()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_filter -> {
                    openFilterDialog()
                    true
                }

                R.id.action_export_analytics -> {
                    showExportFormatChooser()
                    true
                }

                else -> {
                    // no-op
                    true
                }
            }
        }
    }

    private fun setupCharts() {
        // PieChart для категорий
        binding.categoryPieChart.apply {
            description.isEnabled = false
            isRotationEnabled = true
            legend.isEnabled = false
            setNoDataText("Нет данных по категориям")
            setNoDataTextColor(ContextCompat.getColor(context, R.color.chart_no_data_text))
        }

        // BarChart для периодов
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
            setNoDataTextColor(ContextCompat.getColor(context, R.color.chart_no_data_text))
        }

        // BarChart для сравнения проектов
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
            setNoDataTextColor(ContextCompat.getColor(context, R.color.chart_no_data_text))
        }
    }

//    private fun observeFilters() {
//        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
//            viewModel.filter.collect { filter ->
//
//            }
//        }
//    }

    private fun observeOverviewAnalytics() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.filteredOverviewAnalytics.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        showContent()
                        state.data?.let { updateCharts(it) }
                            ?: showFallback("Нет данных аналитики")
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
    }

    private fun showFallback(message: String) {
        binding.progressBar.isVisible = false
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.retry)) { viewModel.startOverviewProjectAnalytics() }
            .show()
    }

    private fun updateCharts(data: OverviewAnalyticsDto) {
        // Период (минимальный и максимальный месяцы из данных)
        val periods = data.periodDistribution.map { it.period }
        val first = periods.firstOrNull().orEmpty()
        val last = periods.lastOrNull().orEmpty()
        binding.tvOverviewSubtitle.text =
            "Аналитика за период: ${first.ifEmpty { "-" }} — ${last.ifEmpty { "-" }}"

        // Общая сумма
        val total = data.totalAmount.toLong()
        binding.tvOverviewTotal.text =
            "Всего потрачено: ${NumberFormat.getInstance().format(total)} ₽"

        updateCategoryChart(data.categoryDistribution)
        updatePeriodChart(data.periodDistribution)
        updateProjectChart(data.projectComparison)
    }


    private fun updateCategoryChart(categories: List<OverviewCategoryStats>) {
        binding.categoryPieChart.apply {
            if (categories.isEmpty()) {
                clear()
                invalidate()
                return
            }

            val sorted = categories.sortedByDescending { it.amount }
            val entries = sorted.map {
                PieEntry(it.amount.toFloat(), it.category)
            }

            val ds = PieDataSet(entries, "").apply {
                colors = getCategoryColors(sorted.size)
                valueFormatter = PercentFormatter(binding.categoryPieChart)
                valueTextSize = 12f
                valueTextColor = Color.BLACK
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1Length = 0.4f
                valueLinePart2Length = 0.4f
            }

            data = PieData(ds).apply {
                setValueTextSize(12f)
            }

            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            setDrawEntryLabels(false)
            holeRadius = 40f
            transparentCircleRadius = 45f

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                textSize = 12f
            }

            animateY(800, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updatePeriodChart(periods: List<OverviewPeriodStats>) {
        binding.periodBarChart.apply {
            if (periods.isEmpty()) {
                clear()
                invalidate()
                return
            }

            val entries = periods.mapIndexed { idx, p ->
                BarEntry(idx.toFloat(), p.amount.toFloat())
            }

            val ds = BarDataSet(entries, "").apply {
                color = ContextCompat.getColor(context, R.color.chart_color_green)
                valueFormatter = LargeValueFormatter()
                valueTextSize = 12f
            }

            data = BarData(ds).apply {
                barWidth = 0.6f
                setValueTextSize(12f)
            }

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(periods.map { it.period })
                textSize = 12f
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                textSize = 12f
            }
            axisRight.isEnabled = false

            animateY(800, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateProjectChart(projects: List<ProjectComparisonStats>) {
        binding.projectComparisonChart.apply {
            if (projects.isEmpty()) {
                clear()
                invalidate()
                return
            }

            val entries = projects.mapIndexed { idx, p ->
                BarEntry(idx.toFloat(), p.amount.toFloat())
            }

            val ds = BarDataSet(entries, "Проекты").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 10f
                valueFormatter = LargeValueFormatter()
            }

            data = BarData(ds)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(projects.map { it.projectName })
                textSize = 12f
            }

            animateY(800, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    // helper для цветов PieChart
    private fun getCategoryColors(count: Int): List<Int> {
        return if (count <= ColorTemplate.VORDIPLOM_COLORS.size) {
            ColorTemplate.VORDIPLOM_COLORS.take(count).toList()
        } else {
            ColorTemplate.MATERIAL_COLORS.toList()
        }
    }

    private fun showExportFormatChooser() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.export_choose_format))
            .setItems(arrayOf("CSV", "PDF")) { _, which ->
                val format =
                    if (which == 0) AnalyticsExportFormat.CSV else AnalyticsExportFormat.PDF
                viewModel.exportAnalytics(format)
            }
            .show()
    }

    private fun collectExportState() =
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showProgress()
                        is UiState.Success -> {
                            hideProgress()
                            state.data?.let { promptSaveOrShare(it) }
                        }

                        is UiState.Error -> {
                            hideProgress()
                            Snackbar
                                .make(binding.root, state.message, Snackbar.LENGTH_LONG)
                                .show()
                        }

                        else -> Unit
                    }
                }
            }
        }

    private val saveDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { safeSaveToUri(it) }
    }

    private fun promptSaveOrShare(file: File) {
        tempFile = file

        AlertDialog.Builder(requireContext())
            .setTitle("Что вы хотите сделать с отчётом?")
            .setItems(arrayOf("Сохранить как…", "Поделиться")) { _, which ->
                when (which) {
                    0 -> promptSaveAs(file.name)
                    1 -> doShare(file)
                }
            }
            .show()
    }

    private fun promptSaveAs(defaultFileName: String) {
        saveDocumentLauncher.launch(defaultFileName)
    }

    private fun safeSaveToUri(uri: Uri) {
        val file = tempFile
        if (file == null || !file.exists() || !file.canRead()) {
            Toast.makeText(requireContext(), "Файл недоступен для сохранения", Toast.LENGTH_SHORT)
                .show()
            return
        }

        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            Toast.makeText(requireContext(), "Файл сохранён", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "Ошибка при сохранении: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun doShare(file: File) {
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(requireContext(), "Файл недоступен для шаринга", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val uri = try {
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            Toast.makeText(
                requireContext(),
                "Невозможно получить URI для файла",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val mime = when (file.extension.lowercase()) {
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(send, "Поделиться отчётом").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(
                requireContext().contentResolver,
                "Отчёт",
                uri
            )
        }

        if (chooser.resolveActivity(requireContext().packageManager) != null) {
            startActivity(chooser)
        } else {
            Toast.makeText(
                requireContext(),
                "Нет приложения для шаринга PDF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showProgress() {
        if (progressDialog == null) {
            progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Экспорт")
                .setMessage("Формируем файл…")
                .setCancelable(false)
                .create()
        }
        progressDialog?.show()
    }

    private fun hideProgress() = progressDialog?.dismiss()

    private fun openFilterDialog() {
        val currentFilter = viewModel.appliedFilter.value
        val state = viewModel.initialOverviewAnalytics.value
        if (state !is UiState.Success) return
        val data = state.data

        val dlgView = layoutInflater.inflate(R.layout.dialog_filter_analytics, null)
        val btnFrom = dlgView.findViewById<MaterialButton>(R.id.btnFromDate)
        val btnTo = dlgView.findViewById<MaterialButton>(R.id.btnToDate)
        val chipGroup = dlgView.findViewById<ChipGroup>(R.id.chipGroupCats)
        val acGran = dlgView.findViewById<AutoCompleteTextView>(R.id.acGranularity)

        fun formatDate(ts: Long?) = ts?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
        } ?: getString(R.string.not_selected)

        var selFrom = currentFilter.fromDate
        var selTo = currentFilter.toDate

        btnFrom.text = getString(R.string.from_date_label, formatDate(selFrom))
        btnFrom.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_start_period))
                .setSelection(selFrom ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { sel ->
                selFrom = sel
                btnFrom.text = getString(R.string.from_date_label, formatDate(selFrom))
            }
            picker.show(parentFragmentManager, "DATE_FROM")
        }

        btnTo.text = getString(R.string.to_date_label, formatDate(selTo))
        btnTo.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_end_period))
                .setSelection(selTo ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { sel ->
                selTo = sel
                btnTo.text = getString(R.string.to_date_label, formatDate(selTo))
            }
            picker.show(parentFragmentManager, "DATE_TO")
        }

        chipGroup.removeAllViews()
        val allCats = data?.categoryDistribution.orEmpty().map { it.category }
        val preselected = currentFilter.categories ?: allCats
        allCats.forEach { cat ->
            chipGroup.addView(
                Chip(
                    requireContext(),
                    null,
                    com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter
                ).apply {
                    id = View.generateViewId()
                    text = cat
                    isCheckable = true
                    isClickable = true
                    isFocusable = true
                    isChecked = preselected.contains(cat)
                })
        }

        val granList = Granularity.entries.map { it.name }
        acGran.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                granList
            )
        )
        acGran.setText(currentFilter.granularity.name, false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.analytics_filters_title)
            .setView(dlgView)
            .setPositiveButton(R.string.apply) { _, _ ->
                val selectedCats = chipGroup.checkedChipIds.map { id ->
                    dlgView.findViewById<Chip>(id).text.toString()
                }
                val categories = selectedCats
                    .takeIf { it.size != allCats.size }  // если все выбраны — значит null
                val gran = Granularity.fromFilter(acGran.text.toString()) ?: Granularity.MONTH

                viewModel.applyFilter(
                    AnalyticsFilter(
                        fromDate = selFrom,
                        toDate = selTo,
                        categories = categories,
                        granularity = gran
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val REQUEST_SAVE = 1002
    }
}
