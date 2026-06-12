package com.example.forgeplan.core.database.entity

import androidx.room.Entity

@Entity(tableName = "project_users", primaryKeys = ["project_id", "user_id"])
data class ProjectUserEntity(
    val project_id: Long,
    val user_id: Long,
    val joined_at: String?,
    val is_synced: Boolean = false
)