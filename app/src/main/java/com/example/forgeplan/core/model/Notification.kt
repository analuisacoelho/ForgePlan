package com.example.forgeplan.core.model

data class Notification(
    val id: Long,
    val user_id: Long?,
    val task_id: Long?,
    val project_id: Long?,
    val type: String?,
    val title: String?,
    val message: String?,
    val is_read: Boolean,
    val created_at: String?
)