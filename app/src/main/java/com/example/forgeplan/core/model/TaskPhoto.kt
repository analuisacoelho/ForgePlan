package com.example.forgeplan.core.model

data class TaskPhoto(
    val id: Long,
    val task_log_id: Long?,
    val photo_url: String?,
    val captured_at: String?,
    val is_synced: Boolean
)