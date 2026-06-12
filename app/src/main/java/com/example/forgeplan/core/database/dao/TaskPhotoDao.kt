package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.TaskPhotoEntity

@Dao
interface TaskPhotoDao {

    @Query("SELECT * FROM task_photos WHERE task_log_id = :logId")
    suspend fun getPhotosByLogId(logId: Long): List<TaskPhotoEntity>

    @Query("SELECT * FROM task_photos WHERE is_synced = 0")
    suspend fun getUnsynced(): List<TaskPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: TaskPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<TaskPhotoEntity>)

    @Query("UPDATE task_photos SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}