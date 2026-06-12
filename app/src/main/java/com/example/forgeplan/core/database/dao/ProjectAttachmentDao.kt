package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.ProjectAttachmentEntity

@Dao
interface ProjectAttachmentDao {

    @Query("SELECT * FROM project_attachments WHERE project_id = :projectId")
    suspend fun getAttachmentsByProjectId(projectId: Long): List<ProjectAttachmentEntity>

    @Query("SELECT * FROM project_attachments WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProjectAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: ProjectAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<ProjectAttachmentEntity>)

    @Query("UPDATE project_attachments SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM project_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}