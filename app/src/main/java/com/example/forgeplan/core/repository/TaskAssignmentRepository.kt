package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskAssignmentRepository {

    fun getAssignmentsByTaskId(
        taskId: Long,
        onSuccess: (List<TaskAssignment>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskAssignmentsByTaskId("eq.$taskId")
            .enqueue(object : Callback<List<TaskAssignment>> {
                override fun onResponse(
                    call: Call<List<TaskAssignment>>,
                    response: Response<List<TaskAssignment>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onError("Erro ao carregar associações: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskAssignment>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun getTaskIdsByUserId(
        userId: Long,
        onSuccess: (List<Long>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskAssignmentsByUserId("eq.$userId")
            .enqueue(object : Callback<List<TaskAssignment>> {
                override fun onResponse(
                    call: Call<List<TaskAssignment>>,
                    response: Response<List<TaskAssignment>>
                ) {
                    if (response.isSuccessful) {
                        val ids = response.body()?.map { it.task_id } ?: emptyList()
                        onSuccess(ids)
                    } else {
                        onError("Erro ao carregar tarefas do utilizador: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskAssignment>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun assignUserToTask(
        assignment: TaskAssignment,
        onSuccess: (TaskAssignment?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.assignUserToTask(assignment)
            .enqueue(object : Callback<List<TaskAssignment>> {
                override fun onResponse(
                    call: Call<List<TaskAssignment>>,
                    response: Response<List<TaskAssignment>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.firstOrNull())
                    } else {
                        if (response.code() == 409) {
                            onError("Este utilizador já está associado à tarefa.")
                        } else {
                            onError("Erro ao associar utilizador à tarefa: ${response.code()}")
                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskAssignment>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun removeUserFromTask(
        taskId: Long,
        userId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.removeUserFromTask(
            taskIdFilter = "eq.$taskId",
            userIdFilter = "eq.$userId"
        ).enqueue(object : Callback<Void> {
            override fun onResponse(
                call: Call<Void>,
                response: Response<Void>
            ) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Erro ao remover utilizador da tarefa: ${response.code()}")
                }
            }

            override fun onFailure(
                call: Call<Void>,
                t: Throwable
            ) {
                onError(t.message ?: "Erro desconhecido")
            }
        })
    }
}