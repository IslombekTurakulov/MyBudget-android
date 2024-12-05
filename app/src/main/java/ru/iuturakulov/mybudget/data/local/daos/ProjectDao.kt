package ru.iuturakulov.mybudget.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity

/**
 * Работа с локальной БД
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects")
    fun getAllProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: Int)
}