package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_attachments")
data class ProjectAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val project_id: Long?,
    val file_url: String?,
    val file_name: String?,
    val file_type: String?,
    val uploaded_at: String?,
    val is_synced: Boolean = false
)