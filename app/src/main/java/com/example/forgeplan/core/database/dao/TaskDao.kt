package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.forgeplan.core.database.entity.TaskEntity

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE project_id = :projectId")
    suspend fun getTasksByProjectId(projectId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getTaskByRemoteId(remoteId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE is_synced = 0")
    suspend fun getUnsynced(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET is_synced = 1, remote_id = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE project_id = :projectId")
    suspend fun deleteByProjectId(projectId: Long)
}