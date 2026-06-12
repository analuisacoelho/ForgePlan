package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task_id: Long?,
    val user_id: Long?,
    val content: String?,
    val created_at: String?,
    val is_synced: Boolean = false
)