package ru.iuturakulov.mybudget.domain.models

import ru.iuturakulov.mybudget.data.local.entities.ProjectStatus

data class ProjectFilter(
  val statuses: Set<ProjectStatus> = ProjectStatus.entries.toSet(), // по умолчанию — все статусы
  val category: String? = null,
  val ownerName: String? = null,
  val minBudget: Double? = null,
  val maxBudget: Double? = null
)