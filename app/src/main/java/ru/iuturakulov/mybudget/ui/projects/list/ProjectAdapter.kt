package ru.iuturakulov.mybudget.ui.projects.list

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.CurrencyFormatter
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.databinding.ItemProjectBinding
import kotlin.math.roundToInt

class ProjectAdapter(
    private val onProjectClicked: (ProjectEntity) -> Unit
) : ListAdapter<ProjectEntity, ProjectAdapter.ProjectViewHolder>(DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val safeColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.budget_safe)
        }
        private val warnColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.budget_warning)
        }
        private val dangerColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.budget_danger)
        }

        fun bind(project: ProjectEntity) = with(binding) {
            with(root.context) {
                tvProjectName.text = project.name
                val description = project.description.takeUnless { it.isNullOrBlank() }
                tvProjectDescription.text = description
                tvProjectDescription.isGone = description == null

                ivCategoryIcon.text = project.categoryIcon ?: "❓"
                ivCategoryIcon.contentDescription =
                    getString(R.string.label_category_project_with_name, project.category)

                tvProjectCategory.text = project.category?.let {
                    getString(
                        R.string.label_category_project_with_name,
                        project.category
                    )
                } ?: getString(R.string.no_category)

                val spent = project.amountSpent
                val limit = project.budgetLimit
                val pct = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0

                // Перекрашиваем индикатор в зависимости от порогов
                val indicatorColor = when {
                    pct < 0.5 -> safeColor    // до 50% — «безопасно»
                    pct < 0.7 -> warnColor    // 50–70% — «предупреждение»
                    else -> dangerColor  // свыше 70% — «критично»
                }
                progressBudget.apply {
                    setIndicatorColor(indicatorColor)
                    setProgress((pct * 100).roundToInt(), true)
                }

                tvBudgetInfo.text = getString(
                    R.string.project_budget_progress,
                    CurrencyFormatter.format(spent),
                    CurrencyFormatter.format(limit),
                    "${(pct * 100).roundToInt()}%"
                )

                val statusColor = project.status.getStatusColor(this)
                val strokeColor = statusColor

                chipStatus.apply {
                    text = project.status.getStatusText(context)

                    chipIcon = ContextCompat.getDrawable(this@with, project.status.getStatusIcon())
                    chipIconTint = ColorStateList.valueOf(statusColor)

                    chipBackgroundColor = ColorStateList.valueOf(
                        ColorUtils.setAlphaComponent(statusColor, (0.1f * 255).toInt())
                    )

                    chipStrokeColor = ColorStateList.valueOf(strokeColor)
                    chipStrokeWidth = 1.0F.dp.value

                    setTextColor(statusColor)
                }

                tvOwner.text = getString(
                    R.string.label_owner_with_name,
                    if (project.ownerName.equals("Вы", ignoreCase = true)) {
                        binding.root.context.getString(R.string.transaction_title_your)
                    } else {
                        project.ownerName
                    }
                )

                // Дата создания
                // tvProjectDate.text = getString(
                //   R.string.label_created_date,
                //   dateFormat.format(Date(project.createdAt))
                // )

                root.setOnClickListener {
                    onProjectClicked(project)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id.hashCode().toLong()


    class DiffCallback : DiffUtil.ItemCallback<ProjectEntity>() {
        override fun areItemsTheSame(a: ProjectEntity, b: ProjectEntity) = a.id == b.id
        override fun areContentsTheSame(a: ProjectEntity, b: ProjectEntity) = a == b
    }
}
