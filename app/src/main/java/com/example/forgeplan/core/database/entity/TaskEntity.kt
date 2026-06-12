package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remote_id: Long? = null,
    val project_id: Long,
    val created_by_id: Long?,
    val title: String,
    val description: String?,
    val status: String?,
    val priority: String?,
    val completion_rate: Int?,
    val start_date: String?,
    val end_date: String?,
    val task_group: String?,
    val is_synced: Boolean = false
)