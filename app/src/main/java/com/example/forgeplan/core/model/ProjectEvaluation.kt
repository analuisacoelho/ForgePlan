package com.example.forgeplan.core.model

data class ProjectEvaluation(
    val id: Long,
    val project_id: Long,
    val user_id: Long?,
    val rating: Int,
    val comment: String?,
    val created_at: String?
)

data class ProjectEvaluationPayload(
    val project_id: Long,
    val user_id: Long?,
    val rating: Int,
    val comment: String?
)