package com.example.forgeplan.core.model

data class ProjectUser(
    val project_user_id: Long,
    val project_id: Long,
    val user_id: Long,
    val joined_at: String?
)

data class ProjectUserPayload(
    val project_id: Long,
    val user_id: Long
)