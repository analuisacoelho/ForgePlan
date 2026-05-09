package com.example.forgeplan.core.model

data class Comment(
    val id: Long,
    val task_id: Long?,
    val user_id: Long?,
    val content: String?,
    val created_at: String?
)