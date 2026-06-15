package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.TaskGroup
import com.example.forgeplan.core.model.TaskGroupPayload
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskGroupRepository {

    fun getGroupsByProjectId(
        projectId: Long,
        onSuccess: (List<TaskGroup>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskGroupsByProjectId("eq.$projectId")
            .enqueue(object : Callback<List<TaskGroup>> {
                override fun onResponse(call: Call<List<TaskGroup>>, response: Response<List<TaskGroup>>) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onSuccess(emptyList())
                        // tratado como lista vazia
                    }
                }
                override fun onFailure(call: Call<List<TaskGroup>>, t: Throwable) {
                    onSuccess(emptyList())
                    // falha de rede silenciada
                }
            })
    }

    fun createGroup(
        projectId: Long,
        name: String,
        onSuccess: (TaskGroup?) -> Unit,
        onError: (String) -> Unit
    ) {
        val payload = TaskGroupPayload(
            project_id = projectId,
            name = name.trim() // remove espaços no início e no fim
        )

        SupabaseApi.service.createTaskGroup(payload)
            .enqueue(object : Callback<List<TaskGroup>> {
                override fun onResponse(
                    call: Call<List<TaskGroup>>,
                    response: Response<List<TaskGroup>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.firstOrNull())
                    } else {
                        onError("Erro ao criar grupo: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskGroup>>,
                    t: Throwable // base de todos os erros
                ) {
                    onError(t.message ?: "Erro desconhecido ao criar grupo.")
                }
            })
    }
}