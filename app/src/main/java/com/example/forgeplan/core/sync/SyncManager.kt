package com.example.forgeplan.core.sync

import android.content.Context
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SyncManager {

    fun syncIfOnline(context: Context) {
        if (!NetworkUtils.isOnline(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseProvider.getDatabase(context)
            syncTaskLogs(context, db)
            syncTaskPhotos(context, db)
            syncComments(context, db)
        }
    }

    // ── Task Logs ─────────────────────────────────────────────────────────────

    private suspend fun syncTaskLogs(context: Context, db: com.example.forgeplan.core.database.AppDatabase) {
        val unsynced = db.taskLogDao().getUnsynced()
        unsynced.forEach { log ->
            try {
                val payload = SupabaseService.TaskLogPayload(
                    task_id = log.task_id,
                    user_id = log.user_id ?: return@forEach,
                    log_date = log.log_date ?: return@forEach,
                    location = log.location ?: "",
                    completion_rate = log.completion_rate ?: 0,
                    minutes_spent = log.minutes_spent ?: 0,
                    notes = log.notes ?: "",
                    is_synced = true
                )
                SupabaseApi.service.insertTaskLog(payload)
                db.taskLogDao().markSynced(log.id)
            } catch (e: Exception) {
                // mantém is_synced = false para tentar novamente
            }
        }
    }

    // ── Task Photos ───────────────────────────────────────────────────────────

    private suspend fun syncTaskPhotos(context: Context, db: com.example.forgeplan.core.database.AppDatabase) {
        val unsynced = db.taskPhotoDao().getUnsynced()
        unsynced.forEach { photo ->
            try {
                val payload = SupabaseService.TaskPhotoPayload(
                    task_log_id = photo.task_log_id ?: return@forEach,
                    photo_url = photo.photo_url ?: return@forEach,
                    captured_at = photo.captured_at ?: return@forEach,
                    is_synced = true
                )
                SupabaseApi.service.insertTaskPhoto(payload)
                db.taskPhotoDao().markSynced(photo.id)
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    private suspend fun syncComments(context: Context, db: com.example.forgeplan.core.database.AppDatabase) {
        val unsynced = db.commentDao().getUnsynced()
        unsynced.forEach { comment ->
            try {
                val payload = SupabaseService.CommentRequest(
                    task_id = comment.task_id ?: return@forEach,
                    user_id = comment.user_id ?: return@forEach,
                    content = comment.content ?: return@forEach
                )
                SupabaseApi.service.insertComment(payload)
                db.commentDao().markSynced(comment.id)
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }
}
