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
        onSuccess: (List<ProjectEvaluation>) -> Unit, // callback com a lista
        onError: (String) -> Unit // callback com mensagem de erroo
    ) {
        SupabaseApi.service // singleton Retrofit que aponta para a API Supabase do projeto
            .getProjectEvaluations("eq.$projectId") // chama o endpoint REST do Supabase
            .enqueue(object : Callback<List<ProjectEvaluation>> { // Supabase devolve sempre um array
                // executa em background para não bloquear a UI
                // Retrofit chama onResponse ou onFailure quando a resposta chega

                override fun onResponse(
                    call: Call<List<ProjectEvaluation>>,
                    response: Response<List<ProjectEvaluation>> // contém código HTTP, headers, e o body deserializado
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList()) // garante que nunca passamos null ao ViewModel
                    } else {
                        onSuccess(emptyList())
                    }
                }

                override fun onFailure( // chamado só em falhas de rede
                    call: Call<List<ProjectEvaluation>>,
                    t: Throwable // erro de rede
                ) {
                    onSuccess(emptyList())
                }
            })
    }

    fun createEvaluation(
        evaluation: ProjectEvaluationPayload, // // objeto com os dados a enviar
        onSuccess: (ProjectEvaluation?) -> Unit,
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
                        onSuccess(response.body()?.firstOrNull()) // pega no primeiro elemento da lista ou null se a lista vier vazia
                    } else {
                        onError("Erro ao guardar avaliação.")
                    }
                }

                override fun onFailure(
                    call: Call<List<ProjectEvaluation>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido ao guardar avaliação.")
                }
            })
    }
}