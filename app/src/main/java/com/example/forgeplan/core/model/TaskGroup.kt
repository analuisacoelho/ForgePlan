package com.example.forgeplan.core.model

data class TaskGroup(
    val id: Long? = null,
    val project_id: Long,
    val name: String,
    val created_at: String? = null
)

data class TaskGroupPayload(
    val project_id: Long,
    val name: String
)