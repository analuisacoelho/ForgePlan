package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.TaskLogEntity
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.TaskPhoto
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService.TaskLogPayload
import com.example.forgeplan.core.network.SupabaseService.TaskPhotoPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskLogRepository {

    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    /**
     * Insere um novo registo em task_logs.
     * Online: envia para o Supabase, devolve o ID remoto.
     * Offline: guarda localmente (is_synced=false), devolve o ID local
     * negativo* para o caller saber que não pode tentar upload de foto.
     *
     * *Como TaskLog.id é Long, devolvemos o id local positivo do Room,
     * mas marcado is_synced=false — o SyncManager fica responsável por
     * enviar depois.
     */
    suspend fun insertTaskLog(payload: TaskLogPayload): Long {
        if (NetworkUtils.isOnline(context)) {
            return try {
                val result = SupabaseApi.service.insertTaskLog(payload)
                result.firstOrNull()?.id ?: saveTaskLogLocally(payload)
            } catch (e: Exception) {
                saveTaskLogLocally(payload)
            }
        } else {
            return saveTaskLogLocally(payload)
        }
    }

    private suspend fun saveTaskLogLocally(payload: TaskLogPayload): Long {
        val entity = TaskLogEntity(
            task_id = payload.task_id,
            user_id = payload.user_id,
            log_date = payload.log_date,
            location = payload.location,
            completion_rate = payload.completion_rate,
            minutes_spent = payload.minutes_spent,
            notes = payload.notes,
            created_at = null,
            is_synced = false
        )
        return db.taskLogDao().insert(entity)
    }

    suspend fun insertTaskPhoto(payload: TaskPhotoPayload): Long {
        return try {
            val result = SupabaseApi.service.insertTaskPhoto(payload)
            result.firstOrNull()?.id ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Obtém todos os logs de uma tarefa específica.
     * Online: busca do Supabase e faz cache em Room.
     * Offline / falha: lê do Room.
     */
    fun getLogsByTaskId(
        taskId: Long,
        onSuccess: (List<TaskLog>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getTaskLogsByTaskId("eq.$taskId")
                .enqueue(object : Callback<List<TaskLog>> {
                    override fun onResponse(
                        call: Call<List<TaskLog>>,
                        response: Response<List<TaskLog>>
                    ) {
                        if (response.isSuccessful) {
                            val logs = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                logs.forEach { remote ->
                                    db.taskLogDao().insert(
                                        TaskLogEntity(
                                            id = remote.id,
                                            task_id = remote.task_id,
                                            user_id = remote.user_id,
                                            log_date = remote.log_date,
                                            location = remote.location,
                                            completion_rate = remote.completion_rate,
                                            minutes_spent = remote.minutes_spent,
                                            notes = remote.notes,
                                            created_at = remote.created_at,
                                            is_synced = true
                                        )
                                    )
                                }
                            }
                            onSuccess(logs)
                        } else {
                            loadLocalLogs(taskId, onSuccess)
                        }
                    }
                    override fun onFailure(call: Call<List<TaskLog>>, t: Throwable) {
                        loadLocalLogs(taskId, onSuccess)
                    }
                })
        } else {
            loadLocalLogs(taskId, onSuccess)
        }
    }

    private fun loadLocalLogs(taskId: Long, onSuccess: (List<TaskLog>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = db.taskLogDao().getLogsByTaskId(taskId).map { it.toModel() }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    fun getPhotosByLogId(
        taskLogId: Long,
        onSuccess: (List<TaskPhoto>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskPhotosByLogId("eq.$taskLogId")
            .enqueue(object : Callback<List<TaskPhoto>> {
                override fun onResponse(
                    call: Call<List<TaskPhoto>>,
                    response: Response<List<TaskPhoto>>
                ) {
                    if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                    else onSuccess(emptyList())
                }
                override fun onFailure(call: Call<List<TaskPhoto>>, t: Throwable) {
                    onSuccess(emptyList())
                }
            })
    }

    private fun TaskLogEntity.toModel() = TaskLog(
        id = id,
        task_id = task_id,
        user_id = user_id,
        log_date = log_date,
        location = location,
        completion_rate = completion_rate,
        minutes_spent = minutes_spent,
        notes = notes,
        created_at = created_at,
        is_synced = is_synced
    )
}