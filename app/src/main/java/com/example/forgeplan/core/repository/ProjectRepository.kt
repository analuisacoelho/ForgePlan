package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectRepository {

    fun getProjects(
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getProjects()
            .enqueue(object : Callback<List<Project>> {

                override fun onResponse(
                    call: Call<List<Project>>,
                    response: Response<List<Project>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onError("Erro ao carregar projetos: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<Project>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun getProjectById(
        projectId: Long,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getProjectById("eq.$projectId")
            .enqueue(object : Callback<List<Project>> {

                override fun onResponse(
                    call: Call<List<Project>>,
                    response: Response<List<Project>>
                ) {
                    if (response.isSuccessful) {

                        val project = response.body()?.firstOrNull()

                        onSuccess(project)

                    } else {
                        onError("Erro ao carregar projeto: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<Project>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}