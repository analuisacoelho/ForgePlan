package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task_id: Long,
    val user_id: Long?,
    val log_date: String?,
    val location: String?,
    val completion_rate: Int?,
    val minutes_spent: Int?,
    val notes: String?,
    val created_at: String?,
    val is_synced: Boolean = false
)