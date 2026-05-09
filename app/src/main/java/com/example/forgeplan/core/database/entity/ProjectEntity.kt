package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Long,
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