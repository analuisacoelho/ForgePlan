
package com.example.forgeplan.core.database.entity

import androidx.room.Entity

@Entity(tableName = "task_assignments", primaryKeys = ["task_id", "user_id"])
data class TaskAssignmentEntity(
    val task_id: Long,
    val user_id: Long,
    val is_synced: Boolean = false
)


