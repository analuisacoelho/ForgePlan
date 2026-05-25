package com.example.forgeplan.core.model

data class TaskAttachment(
    val id: Long,
    val task_id: Long?,
    val file_url: String?,
    val file_name: String?,
    val file_type: String?,
    val uploaded_at: String?,
    val is_synced: Boolean
)

data class TaskAttachmentPayload(
    val task_id: Long,
    val file_url: String,
    val file_name: String,
    val file_type: String?,
    val is_synced: Boolean = true
)