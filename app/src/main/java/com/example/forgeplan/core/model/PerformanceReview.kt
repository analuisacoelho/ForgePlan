package com.example.forgeplan.core.model

data class PerformanceReview(
    val id: Long,
    val project_id: Long?,
    val user_id: Long?,
    val reviewed_by_id: Long?,
    val criteria_id: Long?,
    val score: Int?,
    val notes: String?,
    val reviewed_at: String?
)