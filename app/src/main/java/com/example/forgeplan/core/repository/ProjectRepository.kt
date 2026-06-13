package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.ProjectEntity
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectRepository {

    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    private suspend fun resolveEntity(projectId: Long): ProjectEntity? {
        return db.projectDao().getProjectById(projectId)
            ?: db.projectDao().getProjectByRemoteId(projectId)
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────

    fun getProjects(
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getProjects()
                .enqueue(object : Callback<List<Project>> {
                    override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                        if (response.isSuccessful) {
                            val projects = (response.body() ?: emptyList())
                                .filter { it.status?.uppercase() != "ARCHIVED" }
                            CoroutineScope(Dispatchers.IO).launch {
                                upsertAllFromRemote(projects)
                            }
                            onSuccess(projects)
                        } else {
                            onError("Erro ao carregar projetos: ${response.code()}")
                        }
                    }
                    override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                        loadProjectsFromRoom(onSuccess, onError)
                    }
                })
        } else {
            loadProjectsFromRoom(onSuccess, onError)
        }
    }

    private fun loadProjectsFromRoom(
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = db.projectDao().getAllProjects()
                .filter { it.status?.uppercase() != "ARCHIVED" }
                .map { it.toModel() }
            withContext(Dispatchers.Main) {
                if (local.isNotEmpty()) onSuccess(local)
                else onError("Sem internet e sem dados guardados.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET BY MANAGER ID
    // ─────────────────────────────────────────────────────────────────────

    fun getProjectsByManagerId(
        managerId: Long,
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getProjectsByManagerId("eq.$managerId")
                .enqueue(object : Callback<List<Project>> {
                    override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                        if (response.isSuccessful) {
                            val projects = (response.body() ?: emptyList())
                                .filter { it.status?.uppercase() != "ARCHIVED" }
                            CoroutineScope(Dispatchers.IO).launch {
                                upsertAllFromRemote(projects)
                            }
                            onSuccess(projects)
                        } else onError("Erro ao carregar projetos do manager: ${response.code()}")
                    }
                    override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                        loadLocalByManagerId(managerId, onSuccess)
                    }
                })
        } else {
            loadLocalByManagerId(managerId, onSuccess)
        }
    }

    private fun loadLocalByManagerId(managerId: Long, onSuccess: (List<Project>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val all = db.projectDao().getAllProjects()
            val local = all
                .filter { (it.manager_id == managerId || it.remote_id == managerId) && it.status?.uppercase() != "ARCHIVED" }
                .map { it.toModel() }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET BY ID (id pode ser local ou remoto)
    // ─────────────────────────────────────────────────────────────────────

    fun getProjectById(
        projectId: Long,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getProjectById("eq.$projectId")
                .enqueue(object : Callback<List<Project>> {
                    override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                        if (response.isSuccessful) {
                            val project = response.body()?.firstOrNull()
                            if (project != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    upsertFromRemote(project)
                                }
                                onSuccess(project)
                            } else {
                                loadProjectByIdLocal(projectId, onSuccess)
                            }
                        } else onError("Erro ao carregar projeto: ${response.code()}")
                    }
                    override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                        loadProjectByIdLocal(projectId, onSuccess)
                    }
                })
        } else {
            loadProjectByIdLocal(projectId, onSuccess)
        }
    }

    private fun loadProjectByIdLocal(projectId: Long, onSuccess: (Project?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = resolveEntity(projectId)?.toModel()
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────

    fun createProject(
        project: ProjectPayload,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.createProject(project)
                .enqueue(object : Callback<List<Project>> {
                    override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                        if (response.isSuccessful) {
                            val created = response.body()?.firstOrNull()
                            if (created != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    upsertFromRemote(created)
                                }
                                onSuccess(created)
                            } else onError("Resposta vazia ao criar projeto")
                        } else onError("Erro ao criar projeto: ${response.code()}")
                    }
                    override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                        saveProjectLocallyOnly(project, onSuccess)
                    }
                })
        } else {
            saveProjectLocallyOnly(project, onSuccess)
        }
    }

    private fun saveProjectLocallyOnly(project: ProjectPayload, onSuccess: (Project?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = ProjectEntity(
                id = 0,
                remote_id = null,
                created_by_id = project.created_by_id,
                manager_id = project.manager_id,
                name = project.name,
                description = project.description,
                priority = project.priority,
                status = project.status,
                start_date = project.start_date,
                end_date = project.end_date,
                created_at = null,
                is_synced = false
            )
            val localId = db.projectDao().insert(entity)
            withContext(Dispatchers.Main) {
                onSuccess(entity.copy(id = localId).toModel())
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE (projectId pode ser local ou remoto)
    // ─────────────────────────────────────────────────────────────────────

    fun updateProject(
        projectId: Long,
        project: ProjectPayload,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = resolveEntity(projectId)
            if (existing == null) {
                withContext(Dispatchers.Main) { onError("Projeto não encontrado") }
                return@launch
            }

            val remoteId = existing.remote_id

            if (NetworkUtils.isOnline(context) && remoteId != null) {
                try {
                    val response = SupabaseApi.service.updateProject("eq.$remoteId", project).execute()
                    if (response.isSuccessful) {
                        val updatedEntity = existing.copy(
                            created_by_id = project.created_by_id,
                            manager_id = project.manager_id,
                            name = project.name,
                            description = project.description,
                            priority = project.priority,
                            status = project.status,
                            start_date = project.start_date,
                            end_date = project.end_date,
                            is_synced = true
                        )
                        db.projectDao().update(updatedEntity)
                        withContext(Dispatchers.Main) { onSuccess(updatedEntity.toModel()) }
                    } else {
                        markProjectDirty(existing, project)
                        withContext(Dispatchers.Main) { onError("Erro ao atualizar projeto: ${response.code()}") }
                    }
                } catch (e: Exception) {
                    markProjectDirty(existing, project)
                    withContext(Dispatchers.Main) { onSuccess(existing.toModel()) }
                }
            } else {
                markProjectDirty(existing, project)
                withContext(Dispatchers.Main) { onSuccess(existing.toModel()) }
            }
        }
    }

    private suspend fun markProjectDirty(existing: ProjectEntity, project: ProjectPayload) {
        db.projectDao().update(
            existing.copy(
                created_by_id = project.created_by_id,
                manager_id = project.manager_id,
                name = project.name,
                description = project.description,
                priority = project.priority,
                status = project.status,
                start_date = project.start_date,
                end_date = project.end_date,
                is_synced = false
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // ARCHIVE (soft delete)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * "Elimina" um projeto sem o apagar da base de dados: marca status = "ARCHIVED".
     * Projetos arquivados deixam de aparecer em getProjects/getProjectsByManagerId,
     * mas continuam na BD (histórico, tasks, logs, etc. preservados).
     *
     * projectId pode ser local ou remoto.
     */
    fun archiveProject(
        projectId: Long,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = resolveEntity(projectId)
            if (existing == null) {
                withContext(Dispatchers.Main) { onError("Projeto não encontrado") }
                return@launch
            }

            val payload = ProjectPayload(
                created_by_id = existing.created_by_id,
                manager_id = existing.manager_id,
                name = existing.name,
                description = existing.description,
                priority = existing.priority,
                status = "ARCHIVED",
                start_date = existing.start_date,
                end_date = existing.end_date
            )

            val remoteId = existing.remote_id

            if (NetworkUtils.isOnline(context) && remoteId != null) {
                try {
                    val response = SupabaseApi.service.updateProject("eq.$remoteId", payload).execute()
                    if (response.isSuccessful) {
                        val updatedEntity = existing.copy(status = "ARCHIVED", is_synced = true)
                        db.projectDao().update(updatedEntity)
                        withContext(Dispatchers.Main) { onSuccess(updatedEntity.toModel()) }
                    } else {
                        markProjectDirty(existing, payload)
                        withContext(Dispatchers.Main) { onError("Erro ao arquivar projeto: ${response.code()}") }
                    }
                } catch (e: Exception) {
                    markProjectDirty(existing, payload)
                    withContext(Dispatchers.Main) { onSuccess(existing.copy(status = "ARCHIVED").toModel()) }
                }
            } else {
                markProjectDirty(existing, payload)
                withContext(Dispatchers.Main) { onSuccess(existing.copy(status = "ARCHIVED").toModel()) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // REMOTE <-> LOCAL MAPPING (com mutex para evitar duplicados)
    // ─────────────────────────────────────────────────────────────────────

    private suspend fun upsertAllFromRemote(remoteList: List<Project>) {
        upsertMutex.withLock {
            remoteList.forEach { remote ->
                val existing = db.projectDao().getProjectByRemoteId(remote.id)
                val entity = ProjectEntity(
                    id = existing?.id ?: 0,
                    remote_id = remote.id,
                    created_by_id = remote.created_by_id,
                    manager_id = remote.manager_id,
                    name = remote.name,
                    description = remote.description,
                    priority = remote.priority,
                    status = remote.status,
                    start_date = remote.start_date,
                    end_date = remote.end_date,
                    created_at = remote.created_at,
                    is_synced = true
                )
                if (existing != null) {
                    db.projectDao().update(entity)
                } else {
                    db.projectDao().insert(entity)
                }
            }
        }
    }

    private suspend fun upsertFromRemote(remote: Project) {
        upsertAllFromRemote(listOf(remote))
    }

    companion object {
        private val upsertMutex = Mutex()
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────

    suspend fun getProjectNameById(projectId: Long): String = withContext(Dispatchers.IO) {
        resolveEntity(projectId)?.name ?: "Projeto #$projectId"
    }

    private fun ProjectEntity.toModel() = Project(
        id = remote_id ?: id,
        created_by_id = created_by_id,
        manager_id = manager_id,
        name = name,
        description = description,
        priority = priority,
        status = status,
        start_date = start_date,
        end_date = end_date,
        created_at = created_at
    )

    // ─────────────────────────────────────────────────────────────────────
    // GET ARCHIVED
    // ─────────────────────────────────────────────────────────────────────

    fun getArchivedProjects(
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getProjectsByStatus("eq.ARCHIVED")
                .enqueue(object : Callback<List<Project>> {
                    override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                        if (response.isSuccessful) {
                            val projects = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch { upsertAllFromRemote(projects) }
                            onSuccess(projects)
                        } else {
                            loadArchivedFromRoom(onSuccess, onError)
                        }
                    }
                    override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                        loadArchivedFromRoom(onSuccess, onError)
                    }
                })
        } else {
            loadArchivedFromRoom(onSuccess, onError)
        }
    }

    private fun loadArchivedFromRoom(
        onSuccess: (List<Project>) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = db.projectDao().getAllProjects()
                .filter { it.status?.uppercase() == "ARCHIVED" }
                .map { it.toModel() }
            withContext(Dispatchers.Main) {
                if (local.isNotEmpty()) onSuccess(local)
                else onError("Sem projetos arquivados guardados.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESTORE (unarchive)
    // ─────────────────────────────────────────────────────────────────────
    fun restoreProject(
        projectId: Long,
        onSuccess: (Project?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = resolveEntity(projectId)
            if (existing == null) {
                withContext(Dispatchers.Main) { onError("Projeto não encontrado") }
                return@launch
            }

            val payload = ProjectPayload(
                created_by_id = existing.created_by_id,
                manager_id = existing.manager_id,
                name = existing.name,
                description = existing.description,
                priority = existing.priority,
                status = "ACTIVE",
                start_date = existing.start_date,
                end_date = existing.end_date
            )

            val remoteId = existing.remote_id

            if (NetworkUtils.isOnline(context) && remoteId != null) {
                try {
                    val response = SupabaseApi.service.updateProject("eq.$remoteId", payload).execute()
                    if (response.isSuccessful) {
                        val updated = existing.copy(status = "ACTIVE", is_synced = true)
                        db.projectDao().update(updated)
                        withContext(Dispatchers.Main) { onSuccess(updated.toModel()) }
                    } else {
                        markProjectDirty(existing, payload)
                        withContext(Dispatchers.Main) { onError("Erro ao restaurar: ${response.code()}") }
                    }
                } catch (e: Exception) {
                    markProjectDirty(existing, payload)
                    withContext(Dispatchers.Main) { onSuccess(existing.copy(status = "ACTIVE").toModel()) }
                }
            } else {
                markProjectDirty(existing, payload)
                withContext(Dispatchers.Main) { onSuccess(existing.copy(status = "ACTIVE").toModel()) }
            }
        }
    }
}