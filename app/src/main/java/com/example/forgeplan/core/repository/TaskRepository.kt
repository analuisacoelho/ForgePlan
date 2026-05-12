package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Task
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

                        val task = response.body()?.firstOrNull()

                        onSuccess(task)

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

        SupabaseApi.service.createTask(task)
            .enqueue(object : Callback<Task> {

                override fun onResponse(
                    call: Call<Task>,
                    response: Response<Task>
                ) {

                    if (response.isSuccessful) {

                        onSuccess(response.body())

                    } else {

                        onError("Erro ao criar tarefa: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<Task>,
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
            task = task
        ).enqueue(object : Callback<Task> {

            override fun onResponse(
                call: Call<Task>,
                response: Response<Task>
            ) {

                if (response.isSuccessful) {

                    onSuccess(response.body())

                } else {

                    onError("Erro ao atualizar tarefa: ${response.code()}")
                }
            }

            override fun onFailure(
                call: Call<Task>,
                t: Throwable
            ) {
                onError(t.message ?: "Erro desconhecido")
            }
        })
    }
}