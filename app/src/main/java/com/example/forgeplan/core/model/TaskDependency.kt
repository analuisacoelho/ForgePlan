package com.example.forgeplan.core.model

data class TaskDependency(
    val task_id: Long,
    val depends_on_task_id: Long
)