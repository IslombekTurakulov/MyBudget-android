package ru.iuturakulov.mybudget.ui.analytics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.CurrencyFormatter
import ru.iuturakulov.mybudget.core.DateTimeExtension.toIso8601Date
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity
import ru.iuturakulov.mybudget.data.local.entities.TransactionEntity.TransactionType.Companion.getDisplayRes
import ru.iuturakulov.mybudget.data.remote.dto.AnalyticsExportFormat
import ru.iuturakulov.mybudget.data.remote.dto.CategoryStats
import ru.iuturakulov.mybudget.data.remote.dto.OverviewCategoryStats
import ru.iuturakulov.mybudget.data.remote.dto.OverviewPeriodStats
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.data.remote.dto.PeriodStats
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.transactions.AddTransactionDialogFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Общая логика для обоих экранов аналитики.
 * @param VB   тип ViewBinding
 * @param DTO  тип DTO, который приходит из viewModel
 * @param SC   тип элемента для PieChart (должен маппиться в PieEntry)
 * @param SP   тип элемента для BarChart периодов (мопится в BarEntry)
 */
abstract class BaseAnalyticsFragment<
        VB : ViewBinding,
        DTO,
        SC,
        SP
        >(
    @LayoutRes private val layoutRes: Int,
    private val bindingInflater: (View) -> VB
) : BaseFragment<VB>(layoutRes) {

    override fun getViewBinding(view: View): VB = bindingInflater(view)

    protected abstract var viewModel: AnalyticsViewModel
    protected abstract val analyticsStateFlow: Flow<UiState<DTO>>
    protected abstract val initialAnalyticsStateFlow: Flow<UiState<DTO>>
    protected abstract fun startAnalytics()
    protected abstract fun bindHeader(dto: DTO)
    protected abstract fun bindHeaderChips(dto: DTO)
    protected abstract fun openFilterDialog()

    // для экспорта
    private var tempFile: File? = null
    private var progressDialog: AlertDialog? = null
    private val saveDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { safeSaveToUri(it) }
    }

    override fun setupViews() {
        setupToolbar()
        setupSwipeRefresh()
        setupCharts()
    }

    override fun setupObservers() {
        observeAnalytics()
        collectExportState()
    }

    private fun setupToolbar() {
        val toolbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_filter -> {
                    openFilterDialog()
                    true
                }

                R.id.action_export_analytics -> {
                    showExportFormatChooser()
                    true
                }

                else -> true
            }
        }

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSwipeRefresh() {
        binding.root.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            .setOnRefreshListener { startAnalytics() }
    }

    /** Инициализация общих настроек графиков — описание, легенды, текст «нет данных» */
    protected open fun setupCharts() {
        val noDataText = getString(R.string.no_data)
        val noDataColor = ContextCompat.getColor(requireContext(), R.color.chart_no_data_text)
        val pieChart = binding.root.findViewById<PieChart>(R.id.categoryPieChart)
        val periodBarChart = binding.root.findViewById<BarChart>(R.id.periodBarChart)
        val projectBarChart = binding.root.findViewById<BarChart?>(R.id.projectComparisonChart)

        pieChart.apply {
            description.isEnabled = false
            isRotationEnabled = true
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            setNoDataText(noDataText)
            setNoDataTextColor(noDataColor)

            setTouchEnabled(true)
            isHighlightPerTapEnabled = true

            legend.isEnabled = true
            configureLegendAndOffsets(Legend.LegendForm.CIRCLE)

            animateX(600, Easing.EaseInOutQuad)
            animateY(600, Easing.EaseInOutQuad)
        }

        periodBarChart.apply {
            description.isEnabled = false
            setFitBars(true)
            setNoDataText(noDataText)
            setNoDataTextColor(noDataColor)

            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        CurrencyFormatter.format(value.toDouble())
                }
            }
            axisRight.isEnabled = false

            setTouchEnabled(true)
            setPinchZoom(true)
            isDragEnabled = true

            configureLegendAndOffsets(Legend.LegendForm.LINE)

            animateX(600, Easing.EaseInOutQuad)
            animateY(600, Easing.EaseInOutQuad)
        }

        projectBarChart?.apply {
            description.isEnabled = false
            setFitBars(true)
            setNoDataText(noDataText)
            setNoDataTextColor(noDataColor)

            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        CurrencyFormatter.format(value.toDouble())
                }
            }
            axisRight.isEnabled = false

            setTouchEnabled(true)
            setPinchZoom(true)
            isDragEnabled = true

            legend.isEnabled = false
            configureLegendAndOffsets(Legend.LegendForm.LINE)

            animateX(600, Easing.EaseInOutQuad)
            animateY(600, Easing.EaseInOutQuad)
        }

        val marker = AnalyticsMarkerView(requireContext(), R.layout.view_marker).apply {
            tag = viewModel.currentPeriodLabels
        }
        pieChart.marker = marker
        periodBarChart.marker = marker
        projectBarChart.marker = marker
    }

    private fun observeAnalytics() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                initialAnalyticsStateFlow
                    .filterIsInstance<UiState.Success<DTO>>()
                    .mapNotNull { it.data },
                analyticsStateFlow
                    .filterIsInstance<UiState.Success<DTO>>()
                    .mapNotNull { it.data }
            ) { initialDto, analyticsDto ->
                initialDto to analyticsDto
            }.collectLatest { (initialDto, analyticsDto) ->
                bindHeaderChips(initialDto)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            analyticsStateFlow.collect { state ->
                binding.root.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
                    .isRefreshing = state is UiState.Loading

                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        showContent()
                        state.data?.let {
                            bindHeader(it)
                        }
                    }

                    is UiState.Error -> showError(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun collectExportState() {
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
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                                .show()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    protected fun showLoading() {
        binding.root.findViewById<View>(R.id.progressBar).isVisible = true
    }

    protected fun showContent() {
        binding.root.findViewById<View>(R.id.progressBar).isVisible = false
    }

    protected fun showError(message: String) {
        showContent()
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.retry)) { startAnalytics() }
            .show()
    }

    private fun showExportFormatChooser() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.export_choose_format))
            .setItems(resources.getStringArray(R.array.export_formats)) { _, which ->
                val format = if (which == 0)
                    AnalyticsExportFormat.CSV
                else
                    AnalyticsExportFormat.PDF
                viewModel.exportAnalytics(format)
            }
            .show()
    }

    private fun promptSaveOrShare(file: File) {
        tempFile = file
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.report_actions_title)
            .setItems(
                arrayOf(
                    getString(R.string.report_action_save_as),
                    getString(R.string.report_action_share)
                )
            ) { _, which ->
                if (which == 0) promptSaveAs(file.name)
                else doShare(file)
            }
            .show()
    }

    private fun promptSaveAs(defaultName: String) {
        saveDocumentLauncher.launch(defaultName)
    }

    private fun safeSaveToUri(uri: Uri) {
        val file = tempFile
        if (file == null || !file.exists() || !file.canRead()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.file_unavailable_save),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        requireContext().contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.file_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun doShare(file: File) {
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.file_unavailable_share),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
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
        startActivity(
            Intent.createChooser(send, getString(R.string.share_report_chooser))
                .apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        )
    }

    protected fun showProgress() {
        if (progressDialog == null) {
            progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.export_title))
                .setMessage(getString(R.string.export_progress_message))
                .setCancelable(false)
                .create()
        }
        progressDialog?.show()
    }

    protected fun hideProgress() {
        progressDialog?.dismiss()
    }

    protected fun <C> updateCategoryChart(
        chart: PieChart,
        items: List<C>,
        toEntry: (C) -> PieEntry
    ) {
        if (items.isEmpty()) {
            chart.clear(); chart.invalidate(); return
        }
        val sorted = items.sortedByDescending { toEntry(it).value }
        val entries = sorted.map(toEntry)
        val ds = PieDataSet(entries, "").apply {
            colors = getCategoryColors(sorted.size)
            valueFormatter = PercentFormatter(chart)
            valueTextSize = 12f
            valueTextColor = ContextCompat.getColor(chart.context, android.R.color.black)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
        }
        chart.data = PieData(ds).apply { setValueTextSize(12f) }
        chart.setUsePercentValues(true)
        chart.setDrawEntryLabels(false)
        chart.holeRadius = 40f
        chart.transparentCircleRadius = 45f
        chart.legend.apply {
            isEnabled = true
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            textSize = 12f
        }
        chart.animateY(800, Easing.EaseInOutQuad)
        chart.animateX(800, Easing.EaseInOutQuad)
        chart.invalidate()
    }

    protected fun <P> updatePeriodChart(
        chart: BarChart,
        items: List<P>,
        toEntry: (P) -> BarEntry,
        labels: List<String>
    ) {
        if (items.isEmpty()) {
            chart.clear(); chart.invalidate(); return
        }
        val entries = items.map(toEntry)
        val ds = BarDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueFormatter = LargeValueFormatter()
            valueTextSize = 12f
        }
        chart.data = BarData(ds).apply {
            barWidth = 0.6f
            setValueTextSize(12f)
        }
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            textSize = 12f
        }
        chart.axisLeft.apply {
            axisMinimum = 0f
            granularity = 1f
            textSize = 12f
        }
        chart.legend.apply {
            isEnabled = false
        }
        chart.axisRight.isEnabled = false
        chart.animateY(800, Easing.EaseInOutQuad)
        chart.animateX(800, Easing.EaseInOutQuad)
        chart.invalidate()
    }

    private fun getCategoryColors(count: Int): List<Int> {
        return if (count <= ColorTemplate.VORDIPLOM_COLORS.size) {
            ColorTemplate.VORDIPLOM_COLORS.take(count)
        } else {
            ColorTemplate.MATERIAL_COLORS.toList()
        }
    }

    protected open fun setupOverviewInteractions(
        overviewCategoryStats: List<OverviewCategoryStats>,
        overviewPeriodStats: List<OverviewPeriodStats>,
    ) {
        val pieChart = binding.root.findViewById<PieChart>(R.id.categoryPieChart)
        val periodBarChart = binding.root.findViewById<BarChart>(R.id.periodBarChart)

        pieChart.bindDrillDownList(
            overviewCategoryStats,
            label = { it.category },
            detailItems = { it.transactionInfo },
            detailText = { tx ->
                val formattedAmount = CurrencyFormatter.format(tx.amount)
                val transactionType = TransactionEntity.TransactionType.fromString(tx.type)
                val amount = when (transactionType) {
                    TransactionEntity.TransactionType.EXPENSE -> "-$formattedAmount"
                    TransactionEntity.TransactionType.INCOME -> "+$formattedAmount"
                }
                val projectName = if (tx.projectName != null) {
                    "\n${getString(R.string.project)}: ${tx.projectName}"
                } else {
                    ""
                }
                // например: "12 500 ₽ — Покупка кофе — 2025-04-10"
                "$amount — \"${tx.name}\" — ${tx.date}$projectName"
            },
            clickToDetail = { tx ->
                tx.projectId?.let {
                    AddTransactionDialogFragment.newInstance(
                        projectId = it,
                        currentRole = ParticipantRole.VIEWER.name,
                        transactionId = tx.id
                    ).show(childFragmentManager, "TransactionDetails")
                }
            },
            sheetTag = "sheet_overview_category"
        )

        periodBarChart.bindDrillDown(
            overviewPeriodStats,
            label = { it.period },
            value = { it.amount },
            sheetTag = "sheet_overview_period"
        )
    }

    protected open fun setupProjectInteractions(
        categoryStats: List<CategoryStats>,
        periodStats: List<PeriodStats>,
    ) {
        val pieChart = binding.root.findViewById<PieChart>(R.id.categoryPieChart)
        val periodBarChart = binding.root.findViewById<BarChart>(R.id.periodBarChart)

        pieChart.bindDrillDownList(
            categoryStats,
            label = { it.category },
            detailItems = { it.transactionInfo },
            detailText = { tx ->
                val formattedAmount = CurrencyFormatter.format(tx.amount)
                val transactionType = TransactionEntity.TransactionType.fromString(tx.type)
                val amount = when (transactionType) {
                    TransactionEntity.TransactionType.EXPENSE -> "-$formattedAmount"
                    TransactionEntity.TransactionType.INCOME -> "+$formattedAmount"
                }
                val typeName = transactionType.getDisplayRes(context = requireContext())
                // например: "12 500 ₽ — Покупка кофе — 2025-04-10"
                "$typeName — $amount — \"${tx.name}\" — ${tx.date}"
            },
            clickToDetail = { tx ->
               // Считаем, что пользователь и так находится в проекте, ему не надо смотреть на детализацию
            },
            sheetTag = "sheet_project_category"
        )

        periodBarChart.bindDrillDown(
            periodStats,
            label = { it.period },
            value = { it.totalAmount },
            sheetTag = "sheet_project_period"
        )
    }


    protected fun initCategoryChips(
        chipGroup: ChipGroup,
        categories: List<String>,
        preselect: List<String>,
        clearCharts: () -> Unit,
        onSelectionChanged: (selectedCategories: List<String>?) -> Unit
    ) {
        chipGroup.removeAllViews()
        categories.forEach { cat ->
            chipGroup.addView(
                Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = cat
                    isCheckable = true
                    isClickable = true
                    isChecked = preselect.contains(cat)

                    setOnCheckedChangeListener { _, _ ->
                        clearCharts()
                        val selCats = chipGroup.checkedChipIds.map { id ->
                            chipGroup.findViewById<Chip>(id).text.toString()
                        }
                        val cats = selCats.takeIf { it.size != categories.size }
                        onSelectionChanged(cats)
                    }
                }
            )
        }
    }

    class AnalyticsMarkerView(
        context: Context,
        @LayoutRes layoutId: Int
    ) : MarkerView(context, layoutId) {

        private val tvContent: TextView = findViewById(R.id.tvMarker)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            val text = when (e) {
                is PieEntry -> {
                    context.getString(
                        R.string.marker_pie_format,
                        e.label,
                        CurrencyFormatter.format(e.value.toDouble())
                    )
                }

                is BarEntry -> {
                    val xLabel = (tag as? List<String>)?.getOrNull(e.x.toInt())
                        ?: context.getString(R.string.marker_bar_default_label)
                    context.getString(
                        R.string.marker_bar_format,
                        xLabel,
                        CurrencyFormatter.format(e.y.toDouble())
                    )
                }

                else -> ""
            }
            tvContent.text = text
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF =
            // центрируем горизонтально над точкой, и полностью поднимаем вверх
            MPPointF(-(width / 2f), -height.toFloat())
    }


    /**
     * Универсальная обработка клика на секторе PieChart с выводом списка
     *
     * @param items         исходные модели (например, OverviewCategoryStats)
     * @param label         как из модели получить label (категорию)
     * @param detailItems   как из модели получить список деталей (например, transactionInfo)
     * @param detailText    как из модели-детали собрать текст для каждой строки
     * @param sheetTag      тег для BottomSheet
     */
    protected inline fun <Seg, Tx : Any> PieChart.bindDrillDownList(
        items: List<Seg>,
        crossinline label: (Seg) -> String,
        crossinline detailItems: (Seg) -> List<Tx>?,
        crossinline detailText: (Tx) -> String,
        crossinline clickToDetail: (Tx) -> Unit,
        sheetTag: String
    ) {
        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val pe = e as? PieEntry ?: return
                val stat = items.firstOrNull { label(it) == pe.label } ?: return
                val list = detailItems(stat).orEmpty()
                if (list.isEmpty()) return

                // здесь мы говорим: DetailListBottomSheet< Tx >
                DetailListBottomSheet<Tx>(
                    title = pe.label,
                    items = list
                ) { itemView, tx ->
                    val textView = itemView.findViewById<TextView>(R.id.tvItemText)
                    textView.text = detailText(tx)
                    textView.setOnClickListener { clickToDetail(tx) }
                }.show(childFragmentManager, sheetTag)
            }

            override fun onNothingSelected() {}
        })
    }

    /**
     * Универсальная обработка клика на столбце BarChart.
     *
     * @param items        список ваших моделей
     * @param label        как из модели получить текст метки (периода)
     * @param value        как из модели получить числовое значение
     * @param sheetTag     тег для BottomSheet
     */
    protected inline fun <T> BarChart.bindDrillDown(
        items: List<T>,
        crossinline label: (T) -> String,
        crossinline value: (T) -> Double,
        sheetTag: String
    ) {
        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val be = e as? BarEntry ?: return
                val idx = be.x.toInt().takeIf { it in items.indices } ?: return
                val item = items[idx] ?: return
                DetailListBottomSheet(
                    title = label(item),
                    items = listOf(item)
                ) { itemView, model ->
                    itemView.findViewById<TextView>(R.id.tvItemText).text = getString(
                        R.string.detail_period_format,
                        label(model),
                        CurrencyFormatter.format(value(model))
                    )
                }.show(childFragmentManager, sheetTag)
            }

            override fun onNothingSelected() {}
        })
    }

    protected fun Chart<*>.configureLegendAndOffsets(legendForm: Legend.LegendForm) {
        legend.apply {
            xEntrySpace = resources.getDimension(R.dimen.chart_legend_x_entry_space)
            yEntrySpace = resources.getDimension(R.dimen.chart_legend_y_entry_space)
            // перенос строк если не помещается в одну
            isWordWrapEnabled = true

            // форма маркера (кружок, квадрат, линия)
            form = legendForm
            formSize = resources.getDimension(R.dimen.chart_legend_form_size)
            formToTextSpace = resources.getDimension(R.dimen.chart_legend_form_to_text_space)

            setDrawInside(false)
            orientation = Legend.LegendOrientation.HORIZONTAL
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        }
    }
}