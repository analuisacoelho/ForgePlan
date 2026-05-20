package com.example.forgeplan.core.model

data class Task(
    val id: Long,
    val project_id: Long,
    val created_by_id: Long?,
    val title: String,
    val description: String?,
    val status: String?,
    val priority: String?,
    val completion_rate: Int?,
    val start_date: String?,
    val end_date: String?
)

data class TaskPayload(
    val project_id: Long,
    val created_by_id: Long?,
    val title: String,
    val description: String?,
    val status: String?,
    val priority: String?,
    val completion_rate: Int?,
    val start_date: String?,
    val end_date: String?
)