package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM saved_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<SavedProject>>

    @Query("SELECT * FROM saved_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): SavedProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SavedProject)

    @Update
    suspend fun updateProject(project: SavedProject)

    @Delete
    suspend fun deleteProject(project: SavedProject)

    @Query("DELETE FROM saved_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
