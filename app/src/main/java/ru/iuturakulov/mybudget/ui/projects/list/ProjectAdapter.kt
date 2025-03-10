package ru.iuturakulov.mybudget.ui.projects.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus.Companion.getStatusColor
import ru.iuturakulov.mybudget.databinding.ItemProjectBinding

class ProjectAdapter(
    private val onProjectClicked: (ProjectEntity) -> Unit
) : ListAdapter<ProjectEntity, ProjectAdapter.ProjectViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: ProjectEntity) {
            // Название проекта
            binding.tvProjectName.text = project.name

            // Описание проекта
            binding.tvProjectDescription.text =
                project.description?.ifBlank { "Описание отсутствует" }

            // Финансовые данные
            binding.tvProjectBudget.text = "Бюджет: ${project.budgetLimit} ₽"
            binding.tvProjectSpent.text = "Потрачено: ${project.amountSpent} ₽"
            binding.tvRemainingBudget.text =
                "Осталось: ${(project.budgetLimit - project.amountSpent).coerceAtLeast(0.0)} ₽"

            // Статус проекта
            binding.tvProjectStatus.text = project.status.type
            binding.tvProjectStatus.setTextColor(
                project.status.getStatusColor(context = binding.root.context)
            )

            // Дата создания
            binding.tvProjectDate.text = "Создан: ${project.createdDate}"

            // Обработка нажатия на элемент
            binding.root.setOnClickListener {
                onProjectClicked(project)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProjectEntity>() {
        override fun areItemsTheSame(oldItem: ProjectEntity, newItem: ProjectEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProjectEntity, newItem: ProjectEntity): Boolean {
            return oldItem == newItem
        }
    }
}

