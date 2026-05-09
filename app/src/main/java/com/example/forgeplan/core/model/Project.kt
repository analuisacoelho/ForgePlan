package com.example.forgeplan.core.model

data class Project(
    val id: Long,
    val created_by_id: Long?,
    val manager_id: Long?,
    val name: String,
    val description: String?,
    val priority: String?,
    val status: String?,
    val start_date: String?,
    val end_date: String?,
    val created_at: String?
)