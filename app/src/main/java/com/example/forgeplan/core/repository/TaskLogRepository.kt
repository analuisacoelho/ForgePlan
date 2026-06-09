package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.TaskPhoto
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService.TaskLogPayload
import com.example.forgeplan.core.network.SupabaseService.TaskPhotoPayload
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskLogRepository {

    /**
     * Insere um novo registo em task_logs (suspend → chamado via coroutine).
     * Devolve o ID gerado pelo Supabase (usado para associar as fotos).
     */
    suspend fun insertTaskLog(payload: TaskLogPayload): Long {
        return try {
            val result = SupabaseApi.service.insertTaskLog(payload)
            result.firstOrNull()?.id ?: -1L
        } catch (e: Exception) {
            -1L
        }
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
     */
    fun getLogsByTaskId(
        taskId: Long,
        onSuccess: (List<TaskLog>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskLogsByTaskId("eq.$taskId")
            .enqueue(object : Callback<List<TaskLog>> {
                override fun onResponse(
                    call: Call<List<TaskLog>>,
                    response: Response<List<TaskLog>>
                ) {
                    if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                    else onError("Erro ao carregar logs: ${response.code()}")
                }
                override fun onFailure(call: Call<List<TaskLog>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    /**
     * Obtém todas as fotos de um task_log específico.
     */
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
                    else onError("Erro ao carregar fotos: ${response.code()}")
                }
                override fun onFailure(call: Call<List<TaskPhoto>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}