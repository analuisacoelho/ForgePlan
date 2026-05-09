package com.example.forgeplan.core.model

data class ActivityLog(
    val id: Long,
    val user_id: Long?,
    val action: String?,
    val entity_type: String?,
    val entity_id: Long?,
    val details: String?,
    val created_at: String?
)