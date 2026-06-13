package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.TaskAssignmentEntity

@Dao
interface TaskAssignmentDao {

    @Query("SELECT * FROM task_assignments WHERE task_id = :taskId")
    suspend fun getByTaskId(taskId: Long): List<TaskAssignmentEntity>

    @Query("SELECT * FROM task_assignments WHERE user_id = :userId")
    suspend fun getByUserId(userId: Long): List<TaskAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<TaskAssignmentEntity>)

    @Query("DELETE FROM task_assignments WHERE task_id = :taskId AND user_id = :userId")
    suspend fun delete(taskId: Long, userId: Long)

    @Query("DELETE FROM task_assignments WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    @Query("DELETE FROM task_assignments WHERE task_id = :taskId AND user_id = :userId")
    suspend fun deleteByTaskAndUser(taskId: Long, userId: Long)
}