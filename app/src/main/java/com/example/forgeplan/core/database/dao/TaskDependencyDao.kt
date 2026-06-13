package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.TaskDependencyEntity

@Dao
interface TaskDependencyDao {

    @Query("SELECT * FROM task_dependencies WHERE task_id = :taskId")
    suspend fun getByTaskId(taskId: Long): List<TaskDependencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deps: List<TaskDependencyEntity>)

    @Query("DELETE FROM task_dependencies WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}