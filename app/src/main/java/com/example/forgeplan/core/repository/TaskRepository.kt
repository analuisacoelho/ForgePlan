package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskPayload
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskRepository {

    fun getTasksByProjectId(
        projectId: Long,
        onSuccess: (List<Task>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTasksByProjectId("eq.$projectId")
            .enqueue(object : Callback<List<Task>> {

                override fun onResponse(
                    call: Call<List<Task>>,
                    response: Response<List<Task>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onError("Erro ao carregar tarefas: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<Task>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun getTaskById(
        taskId: Long,
        onSuccess: (Task?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskById("eq.$taskId")
            .enqueue(object : Callback<List<Task>> {

                override fun onResponse(
                    call: Call<List<Task>>,
                    response: Response<List<Task>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.firstOrNull())
                    } else {
                        onError("Erro ao carregar tarefa: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<Task>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun createTask(
        task: Task,
        onSuccess: (Task?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.createTask(task.toPayload())
            .enqueue(object : Callback<List<Task>> {

                override fun onResponse(
                    call: Call<List<Task>>,
                    response: Response<List<Task>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.firstOrNull())
                    } else {
                        onError("Erro ao criar tarefa: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<Task>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun updateTask(
        task: Task,
        onSuccess: (Task?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.updateTask(
            id = "eq.${task.id}",
            task = task.toPayload()
        ).enqueue(object : Callback<List<Task>> {

            override fun onResponse(
                call: Call<List<Task>>,
                response: Response<List<Task>>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.body()?.firstOrNull())
                } else {
                    onError("Erro ao atualizar tarefa: ${response.code()}")
                }
            }

            override fun onFailure(
                call: Call<List<Task>>,
                t: Throwable
            ) {
                onError(t.message ?: "Erro desconhecido")
            }
        })
    }

    private fun Task.toPayload(): TaskPayload {
        return TaskPayload(
            project_id = project_id,
            created_by_id = created_by_id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            completion_rate = completion_rate,
            start_date = start_date,
            end_date = end_date,
            task_group = task_group
        )
    }
}