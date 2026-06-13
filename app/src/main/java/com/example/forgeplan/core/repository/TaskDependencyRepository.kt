package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.TaskDependency
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskDependencyRepository {

    fun getDependencies(
        taskId: Long,
        onSuccess: (List<TaskDependency>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getDependenciesByTaskId("eq.$taskId")
            .enqueue(object : Callback<List<TaskDependency>> {

                override fun onResponse(
                    call: Call<List<TaskDependency>>,
                    response: Response<List<TaskDependency>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onSuccess(emptyList())
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskDependency>>,
                    t: Throwable
                ) {
                    onSuccess(emptyList())
                }
            })
    }

    fun createDependency(
        dependency: TaskDependency,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.createDependency(dependency)
            .enqueue(object : Callback<List<TaskDependency>> {

                override fun onResponse(
                    call: Call<List<TaskDependency>>,
                    response: Response<List<TaskDependency>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Erro ao criar dependência")
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskDependency>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}