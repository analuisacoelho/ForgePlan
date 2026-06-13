package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.forgeplan.core.database.entity.ProjectEntity

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getProjectByRemoteId(remoteId: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET is_synced = 1, remote_id = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}