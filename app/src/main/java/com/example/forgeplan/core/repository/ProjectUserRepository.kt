package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.ProjectUserEntity
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.ProjectUserPayload
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectUserRepository {

    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    fun getProjectUsersByProjectId(
        projectId: Long,
        onSuccess: (List<ProjectUser>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getProjectUsersByProjectId("eq.$projectId")
                .enqueue(object : Callback<List<ProjectUser>> {
                    override fun onResponse(call: Call<List<ProjectUser>>, response: Response<List<ProjectUser>>) {
                        if (response.isSuccessful) {
                            val list = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.projectUserDao().insertAll(list.map {
                                    ProjectUserEntity(
                                        project_id = it.project_id,
                                        user_id = it.user_id,
                                        joined_at = it.joined_at,
                                        is_synced = true
                                    )
                                })
                            }
                            onSuccess(list)
                        } else loadLocalByProjectId(projectId, onSuccess)
                    }
                    override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                        loadLocalByProjectId(projectId, onSuccess)
                    }
                })
        } else {
            loadLocalByProjectId(projectId, onSuccess)
        }
    }

    private fun loadLocalByProjectId(projectId: Long, onSuccess: (List<ProjectUser>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val project = db.projectDao().getProjectById(projectId)
                ?: db.projectDao().getProjectByRemoteId(projectId)

            val candidateIds = mutableSetOf(projectId)
            project?.remote_id?.let { candidateIds.add(it) }
            project?.id?.let { candidateIds.add(it) }

            val local = db.projectUserDao().getByProjectId(projectId)
                .let { rows ->
                    if (rows.isNotEmpty()) rows
                    else candidateIds.flatMap { db.projectUserDao().getByProjectId(it) }
                }
                .distinctBy { it.user_id }
                .map {
                    ProjectUser(project_user_id = 0, project_id = it.project_id, user_id = it.user_id, joined_at = it.joined_at)
                }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    /**
     * Devolve os IDs dos projectos onde o utilizador participa.
     * projectId aqui pode ser local ou remoto - tentamos ambos.
     */
    fun getProjectIdsByUserId(
        userId: Long,
        onSuccess: (List<Long>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getProjectUsersByUserId("eq.$userId")
                .enqueue(object : Callback<List<ProjectUser>> {
                    override fun onResponse(call: Call<List<ProjectUser>>, response: Response<List<ProjectUser>>) {
                        if (response.isSuccessful) {
                            val list = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.projectUserDao().insertAll(list.map {
                                    ProjectUserEntity(
                                        project_id = it.project_id,
                                        user_id = it.user_id,
                                        joined_at = it.joined_at,
                                        is_synced = true
                                    )
                                })
                            }
                            val ids = list.map { it.project_id }
                            onSuccess(ids)
                        } else loadLocalIdsByUserId(userId, onSuccess)
                    }
                    override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                        loadLocalIdsByUserId(userId, onSuccess)
                    }
                })
        } else {
            loadLocalIdsByUserId(userId, onSuccess)
        }
    }

    /**
     * IDs locais (Room project_users) + resolve para os ids "públicos" usados
     * pela UI (remote_id ?: id), via ProjectDao.
     */
    private fun loadLocalIdsByUserId(userId: Long, onSuccess: (List<Long>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val rows = db.projectUserDao().getByUserId(userId)

            val resolvedIds = rows.mapNotNull { row ->
                // row.project_id em project_users foi gravado com o id "público"
                // (remote_id do projeto, vindo do Supabase). Resolve para o id
                // que a UI usa: tenta achar o projeto local por remote_id == row.project_id,
                // senão por id == row.project_id, e devolve remote_id ?: id (igual ao toModel()).
                val project = db.projectDao().getProjectByRemoteId(row.project_id)
                    ?: db.projectDao().getProjectById(row.project_id)
                project?.let { it.remote_id ?: it.id } ?: row.project_id
            }.distinct()

            withContext(Dispatchers.Main) { onSuccess(resolvedIds) }
        }
    }

    fun assignUserToProject(
        projectUser: ProjectUserPayload,
        onSuccess: (ProjectUser?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.assignUserToProject(projectUser)
                .enqueue(object : Callback<List<ProjectUser>> {
                    override fun onResponse(call: Call<List<ProjectUser>>, response: Response<List<ProjectUser>>) {
                        if (response.isSuccessful) onSuccess(response.body()?.firstOrNull())
                        else saveAssignmentLocally(projectUser, onSuccess)
                    }
                    override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                        saveAssignmentLocally(projectUser, onSuccess)
                    }
                })
        } else {
            saveAssignmentLocally(projectUser, onSuccess)
        }
    }

    private fun saveAssignmentLocally(projectUser: ProjectUserPayload, onSuccess: (ProjectUser?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            db.projectUserDao().insertAll(listOf(
                ProjectUserEntity(
                    project_id = projectUser.project_id,
                    user_id = projectUser.user_id,
                    joined_at = null,
                    is_synced = false
                )
            ))
            withContext(Dispatchers.Main) {
                onSuccess(ProjectUser(project_user_id = 0, project_id = projectUser.project_id, user_id = projectUser.user_id, joined_at = null))
            }
        }
    }

    fun removeUserFromProject(
        projectId: Long,
        userId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.removeUserFromProject(
                projectIdFilter = "eq.$projectId",
                userIdFilter = "eq.$userId"
            ).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.projectUserDao().deleteByProjectIdAndUserId(projectId, userId)
                        }
                        onSuccess()
                    } else onError("Erro ao remover: ${response.code()}")
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    CoroutineScope(Dispatchers.IO).launch {
                        db.projectUserDao().deleteByProjectIdAndUserId(projectId, userId)
                    }
                    onSuccess()
                }
            })
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                db.projectUserDao().deleteByProjectIdAndUserId(projectId, userId)
                withContext(Dispatchers.Main) { onSuccess() }
            }
        }
    }
}