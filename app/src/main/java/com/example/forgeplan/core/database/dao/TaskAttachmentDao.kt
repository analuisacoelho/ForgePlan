package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.TaskAttachmentEntity

@Dao
interface TaskAttachmentDao {

    @Query("SELECT * FROM task_attachments WHERE task_id = :taskId")
    suspend fun getAttachmentsByTaskId(taskId: Long): List<TaskAttachmentEntity>

    @Query("SELECT * FROM task_attachments WHERE is_synced = 0")
    suspend fun getUnsynced(): List<TaskAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: TaskAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<TaskAttachmentEntity>)

    @Query("UPDATE task_attachments SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM task_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}