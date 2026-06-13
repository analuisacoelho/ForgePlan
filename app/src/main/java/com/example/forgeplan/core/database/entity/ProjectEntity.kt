package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["remote_id"], unique = true)]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remote_id: Long? = null,
    val created_by_id: Long?,
    val manager_id: Long?,
    val name: String,
    val description: String?,
    val priority: String?,
    val status: String?,
    val start_date: String?,
    val end_date: String?,
    val created_at: String?,
    val is_synced: Boolean = false
)