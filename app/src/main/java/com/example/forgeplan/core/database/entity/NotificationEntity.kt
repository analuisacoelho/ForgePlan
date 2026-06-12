package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val user_id: Long?,
    val task_id: Long?,
    val project_id: Long?,
    val type: String?,
    val title: String?,
    val message: String?,
    val is_read: Boolean = false,
    val created_at: String?
)