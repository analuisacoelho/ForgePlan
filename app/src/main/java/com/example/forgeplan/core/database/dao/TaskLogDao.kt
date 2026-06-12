package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.TaskLogEntity

@Dao
interface TaskLogDao {

    @Query("SELECT * FROM task_logs WHERE task_id = :taskId ORDER BY created_at DESC")
    suspend fun getLogsByTaskId(taskId: Long): List<TaskLogEntity>

    @Query("SELECT * FROM task_logs WHERE is_synced = 0")
    suspend fun getUnsynced(): List<TaskLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TaskLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<TaskLogEntity>)

    @Query("UPDATE task_logs SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM task_logs WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}