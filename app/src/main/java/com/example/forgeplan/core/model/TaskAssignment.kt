package com.example.forgeplan.core.model

data class TaskAssignment(
    val task_id: Long,
    val user_id: Long,
    val assigned_at: String?
)