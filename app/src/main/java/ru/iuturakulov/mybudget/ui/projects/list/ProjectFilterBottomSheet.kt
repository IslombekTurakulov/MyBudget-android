package ru.iuturakulov.mybudget.ui.projects.list

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.google.android.material.slider.RangeSlider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.CurrencyFormatter.roundTo
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus
import ru.iuturakulov.mybudget.databinding.DialogFilterProjectBinding
import ru.iuturakulov.mybudget.domain.models.ProjectFilter
import ru.iuturakulov.mybudget.ui.BaseBottomSheetDialogFragment

@AndroidEntryPoint
class ProjectFilterBottomSheet :
    BaseBottomSheetDialogFragment<DialogFilterProjectBinding>(R.layout.dialog_filter_project) {

    private val vm: ProjectListViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { requireParentFragment().defaultViewModelProviderFactory }
    )

    override fun getViewBinding(view: View): DialogFilterProjectBinding =
        DialogFilterProjectBinding.bind(view)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = parentFragment
            ?: return dismiss()

        val current = vm.projectFilter.value

        binding.cbActive.isChecked = ProjectStatus.ACTIVE in current.statuses
        binding.cbArchived.isChecked = ProjectStatus.ARCHIVED in current.statuses
        binding.cbDeleted.isChecked = ProjectStatus.DELETED in current.statuses

        val rawCats = resources.getStringArray(R.array.project_categories)
            .filterNot { it.equals(getString(R.string.all), ignoreCase = true) }
        val dynCats = vm.projects.value.mapNotNull { it.category }.distinct()
        val cats = listOf(getString(R.string.all)) + (rawCats + dynCats).distinct()
        binding.spinnerCategory.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                cats
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        )
        binding.spinnerCategory.setOnClickListener {
            binding.spinnerCategory.showDropDown()
        }

        // Ставим текущее значение (или "Все")
        val catSel = current.category ?: getString(R.string.all)
        binding.spinnerCategory.setText(catSel, false)

        val owners = listOf(getString(R.string.all)) + vm.projects.value.map { it.ownerName }.distinct()
        binding.spinnerOwner.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                owners.map { owner ->
                    if (owner.equals("Вы", ignoreCase = true)) {
                        binding.root.context.getString(R.string.transaction_title_your)
                    } else {
                        owner
                    }
                }
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        )
        val ownerSel = current.ownerName ?: getString(R.string.all)
        binding.spinnerOwner.setText(ownerSel, false)
        binding.spinnerOwner.setOnClickListener {
            binding.spinnerOwner.showDropDown()
        }

        val budgets = vm.projects.value.map { it.budgetLimit.toFloat() }
        val rawMin = (budgets.minOrNull() ?: 0f).roundTo(2)
        val rawMax = (budgets.maxOrNull() ?: 0f).roundTo(2)

        // Если в списке только одно значение, задаём хоть какой-то «запас»
        val globalMin = rawMin
        val globalMax = if (rawMax > rawMin) rawMax else rawMin + 1f

        var isSyncSliderDisabled = false
        val curMin = (current.minBudget?.toFloat()?.coerceIn(globalMin, globalMax) ?: globalMin).roundTo(2)
        val curMax = (current.maxBudget?.toFloat()?.coerceIn(globalMin, globalMax) ?: globalMax).roundTo(2)
        binding.sliderBudget.apply {
            valueFrom = globalMin
            valueTo = globalMax
            stepSize = 0f
            values = listOf(
                curMin,
                curMax
            )
            addOnChangeListener { _, _, _ ->
                isSyncSliderDisabled = true
                // лочим текстовые поля при перетаскивании
                binding.etMinBudget?.setText(values[0]?.roundTo(2).toString())
                binding.etMaxBudget?.setText(values[1]?.roundTo(2).toString())
                isSyncSliderDisabled = false
            }
        }
        binding.etMinBudget?.setText(curMin.toString())
        binding.etMaxBudget?.setText(curMax.toString())

        // Синхронизируем, если пользователь ввёл число руками
        fun syncSlider() {
            if (isSyncSliderDisabled) return
            val low =
                (binding.etMinBudget?.text.toString().toFloatOrNull()?.coerceIn(globalMin, globalMax)
                    ?: globalMin).roundTo(2)
            val high =
                (binding.etMaxBudget?.text.toString().toFloatOrNull()?.coerceIn(low, globalMax)
                    ?: globalMax).roundTo(2)
            binding.sliderBudget.values = listOf(low, high)
        }

        binding.etMinBudget?.doAfterTextChanged { syncSlider() }
        binding.etMaxBudget?.doAfterTextChanged { syncSlider() }

        binding.btnClear.setOnClickListener {
            vm.setProjectFilter(ProjectFilter())  // сброс всего
            dismiss()
        }
        binding.btnApply.setOnClickListener {
            val minVal = binding.etMinBudget.text.toString().toDoubleOrNull()
            val maxVal = binding.etMaxBudget.text.toString().toDoubleOrNull()
            if (minVal != null && maxVal != null && minVal > maxVal) {
                Snackbar.make(binding.root, R.string.error_min_max_amount, Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val selStatuses = mutableSetOf<ProjectStatus>().apply {
                if (binding.cbActive.isChecked) add(ProjectStatus.ACTIVE)
                if (binding.cbArchived.isChecked) add(ProjectStatus.ARCHIVED)
                if (binding.cbDeleted.isChecked) add(ProjectStatus.DELETED)
                if (isEmpty()) addAll(ProjectStatus.entries)
            }

            val selCat = binding.spinnerCategory.text?.toString()
                ?.takeIf { it.equals(getString(R.string.all), ignoreCase = true).not() }
            val selOwner = binding.spinnerOwner.text?.toString()
                ?.takeIf { it.equals(getString(R.string.all), ignoreCase = true).not() }
            val range = binding.sliderBudget.values
            val minB = range.getOrNull(0)?.toDouble()?.roundTo(2)
            val maxB = range.getOrNull(1)?.toDouble()?.roundTo(2)

            vm.setProjectFilter(
                ProjectFilter(
                    statuses = selStatuses,
                    category = selCat,
                    ownerName = selOwner,
                    minBudget = minB,
                    maxBudget = maxB
                )
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "ProjectFilterBottomSheet"
    }
}
