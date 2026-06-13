package com.example.forgeplan.core.database.entity

import androidx.room.Entity

@Entity(tableName = "task_dependencies", primaryKeys = ["task_id", "depends_on_task_id"])
data class TaskDependencyEntity(
    val task_id: Long,
    val depends_on_task_id: Long
)