package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.TaskEntity
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskPayload
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * CONVENÇÃO DE IDs (Caminho B - sem alterar a UI/ViewModels existentes):
 *
 * - `Task.id` (model usado pela UI) continua a ser `remote_id ?: id` (igual a antes).
 * - `Task.project_id` mantém-se exatamente como vem da UI / Supabase (id remoto do
 *   projeto), tal como no código original. Não há tradução de project_id aqui.
 *
 * - Qualquer `taskId` recebido pode ser LOCAL ou REMOTO. Resolvemos via
 *   `resolveEntity(taskId)`, que tenta primeiro por id local e depois por remote_id.
 *
 * - As mutações (create/update) decidem POST vs PATCH com base em `remote_id`
 *   da entity resolvida.
 */
class TaskRepository {

    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    /**
     * Resolve um TaskEntity a partir de um id que pode ser local ou remoto.
     */
    private suspend fun resolveEntity(taskId: Long): TaskEntity? {
        return db.taskDao().getTaskById(taskId)
            ?: db.taskDao().getTaskByRemoteId(taskId)
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET LIST BY PROJECT (projectId pode ser local ou remoto)
    // ─────────────────────────────────────────────────────────────────────

    fun getTasksByProjectId(
        projectId: Long,
        onSuccess: (List<Task>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getTasksByProjectId("eq.$projectId")
                .enqueue(object : Callback<List<Task>> {
                    override fun onResponse(call: Call<List<Task>>, response: Response<List<Task>>) {
                        if (response.isSuccessful) {
                            val tasks = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                tasks.forEach { upsertFromRemote(it) }
                            }
                            onSuccess(tasks)
                        } else onError("Erro ao carregar tarefas: ${response.code()}")
                    }
                    override fun onFailure(call: Call<List<Task>>, t: Throwable) {
                        loadLocalByProjectId(projectId, onSuccess)
                    }
                })
        } else {
            loadLocalByProjectId(projectId, onSuccess)
        }
    }

    /**
     * Carrega tasks locais cujo project_id corresponde ao projeto indicado.
     * Como TaskEntity.project_id é guardado como veio (id remoto do projeto,
     * na convenção atual), e projectId aqui pode ser local ou remoto, resolvemos
     * primeiro o ProjectEntity correspondente e comparamos pelos dois ids possíveis.
     */
    private fun loadLocalByProjectId(projectId: Long, onSuccess: (List<Task>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val project = db.projectDao().getProjectById(projectId)
                ?: db.projectDao().getProjectByRemoteId(projectId)

            // ids possíveis pelos quais as tasks locais podem referenciar este projeto
            val candidateIds = mutableSetOf(projectId)
            project?.remote_id?.let { candidateIds.add(it) }
            project?.id?.let { candidateIds.add(it) }

            val local = db.taskDao().getAllTasks()
                .filter { it.project_id in candidateIds }
                .map { it.toModel() }

            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET BY ID (taskId pode ser local ou remoto)
    // ─────────────────────────────────────────────────────────────────────

    fun getTaskById(
        taskId: Long,
        onSuccess: (Task?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getTaskById("eq.$taskId")
                .enqueue(object : Callback<List<Task>> {
                    override fun onResponse(call: Call<List<Task>>, response: Response<List<Task>>) {
                        if (response.isSuccessful) {
                            val task = response.body()?.firstOrNull()
                            if (task != null) {
                                CoroutineScope(Dispatchers.IO).launch { upsertFromRemote(task) }
                                onSuccess(task)
                            } else {
                                loadTaskByIdLocal(taskId, onSuccess)
                            }
                        } else onError("Erro ao carregar tarefa: ${response.code()}")
                    }
                    override fun onFailure(call: Call<List<Task>>, t: Throwable) {
                        loadTaskByIdLocal(taskId, onSuccess)
                    }
                })
        } else {
            loadTaskByIdLocal(taskId, onSuccess)
        }
    }

    private fun loadTaskByIdLocal(taskId: Long, onSuccess: (Task?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = resolveEntity(taskId)?.toModel()
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    /**
     * Versão suspend equivalente, sem callbacks. taskId pode ser local ou remoto.
     */
    suspend fun getTaskByIdSuspend(taskId: Long): Task? = withContext(Dispatchers.IO) {
        if (NetworkUtils.isOnline(context)) {
            try {
                val response = SupabaseApi.service.getTaskById("eq.$taskId").execute()
                val remote = response.body()?.firstOrNull()
                if (remote != null) {
                    upsertFromRemote(remote)
                    return@withContext remote
                }
            } catch (e: Exception) {
                // cai para local
            }
        }
        resolveEntity(taskId)?.toModel()
    }

    // ─────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────

    fun createTask(
        task: Task,
        onSuccess: (Task?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.createTask(task.toPayload())
                .enqueue(object : Callback<List<Task>> {
                    override fun onResponse(call: Call<List<Task>>, response: Response<List<Task>>) {
                        if (response.isSuccessful) {
                            val created = response.body()?.firstOrNull()
                            if (created != null) {
                                CoroutineScope(Dispatchers.IO).launch { upsertFromRemote(created) }
                                onSuccess(created)
                            } else onError("Resposta vazia ao criar tarefa")
                        } else onError("Erro ao criar tarefa: ${response.code()}")
                    }
                    override fun onFailure(call: Call<List<Task>>, t: Throwable) {
                        saveTaskLocallyOnly(task, onSuccess)
                    }
                })
        } else {
            saveTaskLocallyOnly(task, onSuccess)
        }
    }

    /**
     * Guarda a task só localmente (remote_id = null, is_synced = false).
     * O SyncManager irá criar isto no Supabase quando houver rede.
     * O Task model devolvido terá id = id LOCAL (porque remote_id é null).
     *
     * project_id é guardado tal como recebido (id remoto do projeto, na convenção
     * atual da app) - o SyncManager usa-o diretamente no payload ao criar a task.
     */
    private fun saveTaskLocallyOnly(task: Task, onSuccess: (Task?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = TaskEntity(
                id = 0, // autogenerate
                remote_id = null,
                project_id = task.project_id,
                created_by_id = task.created_by_id,
                title = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                completion_rate = task.completion_rate,
                start_date = task.start_date,
                end_date = task.end_date,
                task_group = task.task_group,
                is_synced = false
            )
            val localId = db.taskDao().insert(entity)
            withContext(Dispatchers.Main) {
                onSuccess(entity.copy(id = localId).toModel())
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE (task.id pode ser local ou remoto)
    // ─────────────────────────────────────────────────────────────────────

    fun updateTask(
        task: Task,
        onSuccess: (Task?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = resolveEntity(task.id)
            if (existing == null) {
                withContext(Dispatchers.Main) { onError("Tarefa não encontrada") }
                return@launch
            }

            val remoteId = existing.remote_id

            if (NetworkUtils.isOnline(context) && remoteId != null) {
                // Já existe no Supabase -> PATCH direto
                try {
                    val response = SupabaseApi.service.updateTask("eq.$remoteId", task.toPayload()).execute()
                    if (response.isSuccessful) {
                        val updatedEntity = existing.copy(
                            project_id = task.project_id,
                            created_by_id = task.created_by_id,
                            title = task.title,
                            description = task.description,
                            status = task.status,
                            priority = task.priority,
                            completion_rate = task.completion_rate,
                            start_date = task.start_date,
                            end_date = task.end_date,
                            task_group = task.task_group,
                            is_synced = true
                        )
                        db.taskDao().update(updatedEntity)
                        withContext(Dispatchers.Main) { onSuccess(updatedEntity.toModel()) }
                    } else {
                        markTaskDirty(existing, task)
                        withContext(Dispatchers.Main) { onError("Erro ao atualizar tarefa: ${response.code()}") }
                    }
                } catch (e: Exception) {
                    markTaskDirty(existing, task)
                    withContext(Dispatchers.Main) { onSuccess(existing.toModel()) }
                }
            } else {
                // Offline OU task ainda não existe no servidor (remoteId == null)
                // -> guarda local, marca is_synced = false. SyncManager trata do resto.
                markTaskDirty(existing, task)
                withContext(Dispatchers.Main) { onSuccess(existing.toModel()) }
            }
        }
    }

    private suspend fun markTaskDirty(existing: TaskEntity, task: Task) {
        db.taskDao().update(
            existing.copy(
                project_id = task.project_id,
                created_by_id = task.created_by_id,
                title = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                completion_rate = task.completion_rate,
                start_date = task.start_date,
                end_date = task.end_date,
                task_group = task.task_group,
                is_synced = false
                // remote_id mantém-se igual (seja null ou não)
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // REMOTE <-> LOCAL MAPPING
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Faz upsert de uma Task vinda do Supabase: se já existir uma entity local
     * com esse remote_id, atualiza-a (mantém o id local); senão, insere nova.
     */
    private suspend fun upsertFromRemote(remote: Task) {
        val existing = db.taskDao().getTaskByRemoteId(remote.id)

        val entity = TaskEntity(
            id = existing?.id ?: 0,
            remote_id = remote.id,
            project_id = remote.project_id,
            created_by_id = remote.created_by_id,
            title = remote.title,
            description = remote.description,
            status = remote.status,
            priority = remote.priority,
            completion_rate = remote.completion_rate,
            start_date = remote.start_date,
            end_date = remote.end_date,
            task_group = remote.task_group,
            is_synced = true
        )

        if (existing != null) {
            db.taskDao().update(entity)
        } else {
            db.taskDao().insert(entity)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────

    private fun TaskEntity.toModel() = Task(
        id = remote_id ?: id,
        project_id = project_id,
        created_by_id = created_by_id,
        title = title,
        description = description,
        status = status,
        priority = priority,
        completion_rate = completion_rate,
        start_date = start_date,
        end_date = end_date,
        task_group = task_group
    )

    private fun Task.toPayload() = TaskPayload(
        project_id = project_id,
        created_by_id = created_by_id,
        title = title,
        description = description,
        status = status,
        priority = priority,
        completion_rate = completion_rate,
        start_date = start_date,
        end_date = end_date,
        task_group = task_group
    )
}