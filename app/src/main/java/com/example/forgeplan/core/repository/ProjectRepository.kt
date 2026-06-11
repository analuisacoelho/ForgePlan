package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectPayload
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

    fun getProjectsByManagerId(
        managerId: Long,
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getProjectsByManagerId("eq.$managerId")
            .enqueue(object : Callback<List<Project>> {
                override fun onResponse(
                    call: Call<List<Project>>,
                    response: Response<List<Project>>
                ) {
                    if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                    else onError("Erro ao carregar projetos do manager: ${response.code()}")
                }
                override fun onFailure(call: Call<List<Project>>, t: Throwable) {
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
                        onSuccess(response.body()?.firstOrNull())
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

    fun createProject(
        project: ProjectPayload,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.createProject(project)
            .enqueue(object : Callback<List<Project>> {

                override fun onResponse(
                    call: Call<List<Project>>,
                    response: Response<List<Project>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.firstOrNull())
                    } else {
                        onError("Erro ao criar projeto: ${response.code()}")
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

    fun updateProject(
        projectId: Long,
        project: ProjectPayload,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.updateProject(
            id = "eq.$projectId",
            project = project
        ).enqueue(object : Callback<List<Project>> {

            override fun onResponse(
                call: Call<List<Project>>,
                response: Response<List<Project>>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.body()?.firstOrNull())
                } else {
                    onError("Erro ao atualizar projeto: ${response.code()}")
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