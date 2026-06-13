package com.example.forgeplan.core.sync

import android.content.Context
import com.example.forgeplan.core.database.AppDatabase
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.model.TaskAttachmentPayload
import com.example.forgeplan.core.model.TaskPayload
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * CONVENÇÃO DE IDs (Caminho B):
 * - Entities locais: `id` = id local (Room), `remote_id` = id no Supabase (null se não sincronizado).
 * - `TaskEntity.project_id` é guardado como o id REMOTO do projeto (igual ao comportamento
 *   original da app) - EXCETO no caso especial de uma task ter sido criada offline
 *   associada a um projeto que TAMBÉM foi criado offline. Nesse caso, no momento da
 *   criação, o único id de projeto disponível é o id LOCAL do projeto, por isso
 *   `task.project_id` fica temporariamente a apontar para esse id local.
 *
 * ORDEM DE SINCRONIZAÇÃO:
 * 1. Projects -> obtêm remote_id
 * 1.5. Corrigir project_id das tasks que apontavam para o id LOCAL de projetos
 *      que acabaram de ser sincronizados (ver fixTaskProjectIdsAfterProjectSync)
 * 2. Tasks
 * 3. Task logs / photos / comments / attachments
 */
object SyncManager {

    fun syncIfOnline(context: Context) {
        if (!NetworkUtils.isOnline(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseProvider.getDatabase(context)
            val syncedProjects = syncProjects(db)
            fixTaskProjectIdsAfterProjectSync(db, syncedProjects)
            syncTasks(db)
            syncTaskLogs(db)
            syncTaskPhotos(db)
            syncComments(db)
            syncTaskAttachments(db)
            syncProjectAttachments(db)
        }
    }

    /** Par (localId do projeto, remote_id atribuído) para projetos sincronizados nesta passagem. */
    private data class SyncedProject(val localId: Long, val remoteId: Long)

    // ── Projects ──────────────────────────────────────────────────────────────

    private suspend fun syncProjects(db: AppDatabase): List<SyncedProject> {
        val synced = mutableListOf<SyncedProject>()
        val unsynced = db.projectDao().getUnsynced()
        unsynced.forEach { project ->
            try {
                val payload = ProjectPayload(
                    created_by_id = project.created_by_id,
                    manager_id = project.manager_id,
                    name = project.name,
                    description = project.description,
                    priority = project.priority,
                    status = project.status,
                    start_date = project.start_date,
                    end_date = project.end_date
                )
                if (project.remote_id == null) {
                    // Criar no Supabase
                    val response = SupabaseApi.service.createProject(payload).execute()
                    val remoteId = response.body()?.firstOrNull()?.id
                    if (remoteId != null) {
                        db.projectDao().markSynced(project.id, remoteId)
                        synced.add(SyncedProject(localId = project.id, remoteId = remoteId))
                    }
                } else {
                    // Atualizar no Supabase
                    val response = SupabaseApi.service.updateProject("eq.${project.remote_id}", payload).execute()
                    if (response.isSuccessful) {
                        db.projectDao().markSynced(project.id, project.remote_id)
                    }
                }
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
        return synced
    }

    /**
     * Para cada projeto recém-sincronizado (localId -> remoteId), atualiza todas as
     * TaskEntity cujo project_id ainda apontava para o `localId` do projeto,
     * substituindo por `remoteId`. Isto cobre o caso de "projeto criado offline +
     * tarefas criadas offline para esse projeto".
     *
     * Tasks que já tinham project_id == remoteId (caso normal) não são afetadas.
     */
    private suspend fun fixTaskProjectIdsAfterProjectSync(db: AppDatabase, syncedProjects: List<SyncedProject>) {
        if (syncedProjects.isEmpty()) return

        val allTasks = db.taskDao().getAllTasks()
        syncedProjects.forEach { sp ->
            allTasks
                .filter { it.project_id == sp.localId }
                .forEach { task ->
                    db.taskDao().update(task.copy(project_id = sp.remoteId))
                }
        }
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    private suspend fun syncTasks(db: AppDatabase) {
        val unsynced = db.taskDao().getUnsynced()
        unsynced.forEach { task ->
            try {
                // Após fixTaskProjectIdsAfterProjectSync, task.project_id já deve ser o
                // remote_id do projeto. Se ainda assim corresponder a um id local de um
                // projeto NÃO sincronizado (sem remote_id), salta esta task para a
                // próxima passagem de sync.
                val projectStillUnsynced = db.projectDao().getProjectById(task.project_id)?.let {
                    it.remote_id == null
                } ?: false

                if (projectStillUnsynced) {
                    return@forEach
                }

                val payload = TaskPayload(
                    project_id = task.project_id,
                    created_by_id = task.created_by_id,
                    title = task.title,
                    description = task.description,
                    status = task.status,
                    priority = task.priority,
                    completion_rate = task.completion_rate,
                    start_date = task.start_date,
                    end_date = task.end_date,
                    task_group = task.task_group
                )

                if (task.remote_id == null) {
                    // Criar no Supabase
                    val response = SupabaseApi.service.createTask(payload).execute()
                    val remoteId = response.body()?.firstOrNull()?.id
                    if (remoteId != null) {
                        db.taskDao().markSynced(task.id, remoteId)
                    }
                } else {
                    // Atualizar no Supabase
                    val response = SupabaseApi.service.updateTask("eq.${task.remote_id}", payload).execute()
                    if (response.isSuccessful) {
                        db.taskDao().markSynced(task.id, task.remote_id)
                    }
                }
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }

    // ── Task Logs ─────────────────────────────────────────────────────────────

    private suspend fun syncTaskLogs(db: AppDatabase) {
        val unsynced = db.taskLogDao().getUnsynced()
        unsynced.forEach { log ->
            try {
                // log.task_id pode ser local ou remoto (depende de quando foi criado).
                // Resolvemos a task correspondente e usamos o remote_id para o payload.
                val task = db.taskDao().getTaskById(log.task_id)
                    ?: db.taskDao().getTaskByRemoteId(log.task_id)

                val remoteTaskId = task?.remote_id ?: return@forEach

                val payload = SupabaseService.TaskLogPayload(
                    task_id = remoteTaskId,
                    user_id = log.user_id ?: return@forEach,
                    log_date = log.log_date ?: return@forEach,
                    location = log.location ?: "",
                    completion_rate = log.completion_rate ?: 0,
                    minutes_spent = log.minutes_spent ?: 0,
                    notes = log.notes ?: "",
                    is_synced = true
                )
                SupabaseApi.service.insertTaskLog(payload)
                db.taskLogDao().markSynced(log.id)
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }

    // ── Task Photos ───────────────────────────────────────────────────────────

    private suspend fun syncTaskPhotos(db: AppDatabase) {
        val unsynced = db.taskPhotoDao().getUnsynced()
        unsynced.forEach { photo ->
            try {
                val logId = photo.task_log_id ?: return@forEach

                // A foto só pode ser enviada depois do task_log a que pertence
                // estar sincronizado (senão o Supabase não tem esse task_log_id).
                val log = db.taskLogDao().getLogsByTaskId(logId).firstOrNull { it.id == logId }
                if (log == null || !log.is_synced) {
                    return@forEach
                }

                val payload = SupabaseService.TaskPhotoPayload(
                    task_log_id = logId,
                    photo_url = photo.photo_url ?: return@forEach,
                    captured_at = photo.captured_at ?: return@forEach,
                    is_synced = true
                )
                SupabaseApi.service.insertTaskPhoto(payload)
                db.taskPhotoDao().markSynced(photo.id)
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    private suspend fun syncComments(db: AppDatabase) {
        val unsynced = db.commentDao().getUnsynced()
        unsynced.forEach { comment ->
            try {
                val taskIdRef = comment.task_id ?: return@forEach
                val task = db.taskDao().getTaskById(taskIdRef)
                    ?: db.taskDao().getTaskByRemoteId(taskIdRef)

                val remoteTaskId = task?.remote_id ?: return@forEach

                val payload = SupabaseService.CommentRequest(
                    task_id = remoteTaskId,
                    user_id = comment.user_id ?: return@forEach,
                    content = comment.content ?: return@forEach
                )
                SupabaseApi.service.insertComment(payload)
                db.commentDao().markSynced(comment.id)
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }

    // ── Task Attachments ─────────────────────────────────────────────────────

    private suspend fun syncTaskAttachments(db: AppDatabase) {
        val unsynced = db.taskAttachmentDao().getUnsynced()
        unsynced.forEach { attachment ->
            try {
                val taskIdRef = attachment.task_id ?: return@forEach
                val task = db.taskDao().getTaskById(taskIdRef)
                    ?: db.taskDao().getTaskByRemoteId(taskIdRef)

                val remoteTaskId = task?.remote_id ?: return@forEach

                val payload = TaskAttachmentPayload(
                    task_id = remoteTaskId,
                    file_url = attachment.file_url ?: return@forEach,
                    file_name = attachment.file_name ?: "",
                    file_type = attachment.file_type,
                    is_synced = true
                )
                val response = SupabaseApi.service.createTaskAttachment(payload).execute()
                if (response.isSuccessful) {
                    db.taskAttachmentDao().markSynced(attachment.id)
                }
            } catch (e: Exception) {
                // mantém is_synced = false
            }
        }
    }

    // ── Project Attachments ──────────────────────────────────────────────────

    private suspend fun syncProjectAttachments(db: AppDatabase) {
        // SupabaseService ainda não tem endpoint POST para project_attachments.
        // Quando existir, seguir o mesmo padrão de syncTaskAttachments, resolvendo
        // attachment.project_id (local ou remoto) -> project.remote_id.
        val unsynced = db.projectAttachmentDao().getUnsynced()
        if (unsynced.isNotEmpty()) {
            // TODO: implementar quando createProjectAttachment existir no SupabaseService
        }
    }
}