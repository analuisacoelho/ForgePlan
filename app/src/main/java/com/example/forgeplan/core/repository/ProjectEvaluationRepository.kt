package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.ProjectEvaluation
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectEvaluationRepository {

    fun getEvaluations(
        projectId: Long,
        onSuccess: (List<ProjectEvaluation>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service
            .getProjectEvaluations("eq.$projectId")
            .enqueue(object : Callback<List<ProjectEvaluation>> {

                override fun onResponse(
                    call: Call<List<ProjectEvaluation>>,
                    response: Response<List<ProjectEvaluation>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onError("Erro ao carregar avaliações")
                    }
                }

                override fun onFailure(
                    call: Call<List<ProjectEvaluation>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun createEvaluation(
        evaluation: ProjectEvaluationPayload,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service
            .createProjectEvaluation(evaluation)
            .enqueue(object : Callback<List<ProjectEvaluation>> {

                override fun onResponse(
                    call: Call<List<ProjectEvaluation>>,
                    response: Response<List<ProjectEvaluation>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Erro ao guardar avaliação")
                    }
                }

                override fun onFailure(
                    call: Call<List<ProjectEvaluation>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}