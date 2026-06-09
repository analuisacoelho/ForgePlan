package com.example.forgeplan.core.model

data class TaskLog(
    val id: Long,
    val task_id: Long,
    val user_id: Long?,
    val log_date: String?,
    val location: String?,
    val completion_rate: Int?,
    val minutes_spent: Int?,
    val notes: String?,
    val created_at: String?,
    val is_synced: Boolean
)
