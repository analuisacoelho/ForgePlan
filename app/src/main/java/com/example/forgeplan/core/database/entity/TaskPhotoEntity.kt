package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_photos")
data class TaskPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task_log_id: Long?,
    val photo_url: String?,
    val captured_at: String?,
    val is_synced: Boolean = false
)