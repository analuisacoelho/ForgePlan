package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.ProjectUserPayload
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectUserRepository {

    fun getProjectUsersByProjectId(
        projectId: Long,
        onSuccess: (List<ProjectUser>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getProjectUsersByProjectId("eq.$projectId")
            .enqueue(object : Callback<List<ProjectUser>> {
                override fun onResponse(call: Call<List<ProjectUser>>, response: Response<List<ProjectUser>>) {
                    if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                    else onError("Erro ao carregar utilizadores do projeto: ${response.code()}")
                }
                override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    /**
     * Devolve os IDs dos projectos onde o utilizador participa.
     * Consultado pelo UserDashboardViewModel para filtrar projectos.
     */
    fun getProjectIdsByUserId(
        userId: Long,
        onSuccess: (List<Long>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getProjectUsersByUserId("eq.$userId")
            .enqueue(object : Callback<List<ProjectUser>> {
                override fun onResponse(call: Call<List<ProjectUser>>, response: Response<List<ProjectUser>>) {
                    if (response.isSuccessful) {
                        val ids = response.body()?.map { it.project_id } ?: emptyList()
                        onSuccess(ids)
                    } else {
                        onError("Erro ao carregar projectos do utilizador: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    fun assignUserToProject(
        projectUser: ProjectUserPayload,
        onSuccess: (ProjectUser?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.assignUserToProject(projectUser)
            .enqueue(object : Callback<List<ProjectUser>> {
                override fun onResponse(call: Call<List<ProjectUser>>, response: Response<List<ProjectUser>>) {
                    if (response.isSuccessful) onSuccess(response.body()?.firstOrNull())
                    else onError("Erro ao associar utilizador ao projeto: ${response.code()}")
                }
                override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}