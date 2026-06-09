package com.example.forgeplan.progress.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService
import com.example.forgeplan.core.network.SupabaseService.TaskLogPayload
import com.example.forgeplan.core.network.SupabaseService.TaskPhotoPayload
import com.example.forgeplan.core.repository.TaskLogRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

private const val TAG = "ProgressViewModel"

class ProgressViewModel : ViewModel() {

    private val taskLogRepo = TaskLogRepository()
    private val taskRepo    = TaskRepository()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult

    fun clearResult() { _saveResult.value = null }

    fun saveProgress(
        task: Task,
        logDate: String,
        location: String,
        completionRate: Int,
        minutesSpent: Int,
        notes: String,
        photoUri: Uri?,
        context: Context,
        successMsg: String,
        errorPrefix: String
    ) {
        val userId = SessionManager.userId
        if (userId == -1L) {
            _saveResult.value = "$errorPrefix: sessão inválida"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // 1. Atualiza a task
                val newStatus = when {
                    completionRate >= 100 -> "Done"
                    completionRate > 0    -> "IN_PROGRESS"
                    else                  -> "PENDING"
                }
                val updatedTask = task.copy(
                    completion_rate = completionRate,
                    status          = newStatus,
                    start_date      = logDate.ifBlank { task.start_date }
                )
                val taskOk = suspendCancellableCoroutine { cont ->
                    taskRepo.updateTask(
                        task      = updatedTask,
                        onSuccess = { cont.resume(true) },
                        onError   = { msg -> Log.e(TAG, "task update error: $msg"); cont.resume(false) }
                    )
                }
                if (!taskOk) {
                    _saveResult.value = "$errorPrefix (task update)"
                    return@launch
                }

                // 2. Insere task_log
                val nowIso = Instant.now()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val logPayload = TaskLogPayload(
                    task_id         = task.id,
                    user_id         = userId,
                    log_date        = logDate.ifBlank { nowIso.substring(0, 10) },
                    location        = location.ifBlank { "" },
                    completion_rate = completionRate,
                    minutes_spent   = minutesSpent * 60,
                    notes           = notes,
                    is_synced       = true
                )
                val logId = taskLogRepo.insertTaskLog(logPayload)
                Log.d(TAG, "task_log inserted with id=$logId")

                if (logId == -1L) {
                    _saveResult.value = successMsg
                    return@launch
                }

                // 3. Upload de foto (se existir)
                if (photoUri != null) {
                    uploadPhoto(
                        photoUri    = photoUri,
                        context     = context,
                        taskId      = task.id,
                        logId       = logId,
                        nowIso      = nowIso,
                        successMsg  = successMsg,
                        errorPrefix = errorPrefix
                    )
                } else {
                    _saveResult.value = successMsg
                }

            } catch (e: Exception) {
                Log.e(TAG, "saveProgress error", e)
                _saveResult.value = "$errorPrefix: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun uploadPhoto(
        photoUri: Uri,
        context: Context,
        taskId: Long,
        logId: Long,
        nowIso: String,
        successMsg: String,
        errorPrefix: String
    ) {
        try {
            Log.d(TAG, "=== uploadPhoto START === taskId=$taskId logId=$logId")

            // Leitura de bytes em IO
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver
                    .openInputStream(photoUri)
                    ?.use { it.readBytes() }
            }

            if (bytes == null || bytes.isEmpty()) {
                Log.w(TAG, "photo bytes are null or empty")
                _saveResult.value = "$successMsg (⚠ foto vazia)"
                return
            }

            Log.d(TAG, "bytes read: ${bytes.size}")

            val mimeType = context.contentResolver.getType(photoUri) ?: "image/jpeg"
            val ext = when (mimeType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png"               -> "png"
                "image/gif"               -> "gif"
                "image/webp"              -> "webp"
                else -> mimeType.substringAfter("/").substringBefore(";").take(10)
            }
            val filePath = "task_${taskId}/log_${logId}_${System.currentTimeMillis()}.$ext"

            Log.d(TAG, "mimeType=$mimeType filePath=$filePath")

            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

            // Upload em IO — corrige o NetworkOnMainThreadException
            val response = withContext(Dispatchers.IO) {
                SupabaseApi.storageService.uploadFile(
                    bucket      = "task-photos",
                    filePath    = filePath,
                    contentType = mimeType,
                    file        = body
                ).execute()
            }

            Log.d(TAG, "upload response: code=${response.code()} success=${response.isSuccessful}")

            if (response.isSuccessful) {
                response.body()?.close()

                val publicUrl = SupabaseApi.publicStorageUrl("task-photos", filePath)
                Log.d(TAG, "publicUrl=$publicUrl")

                // Insert task_photo em IO
                withContext(Dispatchers.IO) {
                    taskLogRepo.insertTaskPhoto(
                        TaskPhotoPayload(
                            task_log_id = logId,
                            photo_url   = publicUrl,
                            captured_at = nowIso,
                            is_synced   = true
                        )
                    )
                }

                _saveResult.value = successMsg

            } else {
                val errBody = response.errorBody()?.string() ?: "sem detalhe"
                Log.e(TAG, "upload FAILED: code=${response.code()} body=$errBody")
                _saveResult.value = "$successMsg (⚠ foto: ${response.code()} $errBody)"
            }

        } catch (e: Exception) {
            Log.e(TAG, "uploadPhoto EXCEPTION: ${e::class.java.simpleName}: ${e.message}", e)
            _saveResult.value = "$successMsg (⚠ foto não carregada: ${e::class.java.simpleName} - ${e.message})"
        }
    }
}