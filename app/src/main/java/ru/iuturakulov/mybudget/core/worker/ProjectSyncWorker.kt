package ru.iuturakulov.mybudget.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository

class ProjectSyncWorker(
    @ApplicationContext private val context: Context,
    workerParams: WorkerParameters,
    private val projectRepository: ProjectRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            projectRepository.syncProjects()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
