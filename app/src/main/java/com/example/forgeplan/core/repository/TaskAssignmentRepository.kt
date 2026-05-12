package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskAssignmentRepository {

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
}