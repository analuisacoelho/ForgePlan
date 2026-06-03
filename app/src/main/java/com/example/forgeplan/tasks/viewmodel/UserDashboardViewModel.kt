package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.repository.ProjectRepository
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.TaskAssignmentRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService.CommentRequest
/**
 * Localização: app/src/main/java/com/example/forgeplan/tasks/viewmodel/UserDashboardViewModel.kt
 * SUBSTITUI completamente o ficheiro existente.
 *
 * Lógica:
 *  1. project_users filtrado por user_id → IDs dos projectos
 *  2. Para cada project_id → busca o Project completo
 *  3. task_assignments filtrado por user_id → IDs das tarefas
 *  4. Para cada projecto → intersecção de tarefas do projecto com as atribuídas ao user
 *  5. Expõe projectsWithTasks: Map<Project, List<Task>>
 *
 * Novo método updateTaskProgress: usado pelo ProgressScreen para guardar
 * data, taxa de conclusão e status sem recarregar o dashboard inteiro.
 */
class UserDashboardViewModel : ViewModel() {

    private val projectUserRepo    = ProjectUserRepository()
    private val projectRepo        = ProjectRepository()
    private val taskAssignmentRepo = TaskAssignmentRepository()
    private val taskRepo           = TaskRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _projectsWithTasks = MutableStateFlow<Map<Project, List<Task>>>(emptyMap())
    val projectsWithTasks: StateFlow<Map<Project, List<Task>>> = _projectsWithTasks

    // ── Carregamento principal ───────────────────────────────────────────────

    fun loadDashboard() {
        val userId = SessionManager.userId
        if (userId == -1L) {
            _error.value = "Sessão inválida. Faz login novamente."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val projectIds = fetchProjectIds(userId)
                val projects   = projectIds.mapNotNull { fetchProject(it) }

                val result = linkedMapOf<Project, List<Task>>()
                for (project in projects) {
                    result[project] = fetchUserTasksForProject(userId, project.id)
                }
                _projectsWithTasks.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Marcar como feita (UserDashboardScreen) ──────────────────────────────

    fun markTaskAsDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(status = "Done", completion_rate = 100)
            suspendCancellableCoroutine { cont ->
                taskRepo.updateTask(task = updated, onSuccess = { cont.resume(Unit) }, onError = { cont.resume(Unit) })
            }
            loadDashboard()
        }
    }

    // ── Guardar progresso (ProgressScreen) ───────────────────────────────────

    /**
     * Actualiza uma tarefa com o novo status, taxa de conclusão e data.
     * Ao contrário de markTaskAsDone, não recarrega o dashboard inteiro —
     * apenas actualiza a tarefa no mapa em memória para a UI reflectir
     * a mudança imediatamente, evitando um reload desnecessário.
     */
    fun updateTaskProgress(
        task: Task,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val ok = suspendCancellableCoroutine { cont ->
                taskRepo.updateTask(
                    task      = task,
                    onSuccess = { cont.resume(true) },
                    onError   = { msg -> onError(msg); cont.resume(false) }
                )
            }
            if (ok) {
                // Actualiza apenas a tarefa no mapa sem reload completo
                val updated = _projectsWithTasks.value.mapValues { (_, tasks) ->
                    tasks.map { if (it.id == task.id) task else it }
                }
                _projectsWithTasks.value = updated
                onSuccess()
            }
        }
    }

    // ── Helpers suspensos ────────────────────────────────────────────────────

    private suspend fun fetchProjectIds(userId: Long): List<Long> =
        suspendCancellableCoroutine { cont ->
            projectUserRepo.getProjectIdsByUserId(
                userId    = userId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }

    private suspend fun fetchProject(projectId: Long): Project? =
        suspendCancellableCoroutine { cont ->
            projectRepo.getProjectById(
                projectId = projectId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(null) }
            )
        }

    private suspend fun fetchUserTasksForProject(userId: Long, projectId: Long): List<Task> {
        val assignedIds: List<Long> = suspendCancellableCoroutine { cont ->
            taskAssignmentRepo.getTaskIdsByUserId(
                userId    = userId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }
        if (assignedIds.isEmpty()) return emptyList()

        val projectTasks: List<Task> = suspendCancellableCoroutine { cont ->
            taskRepo.getTasksByProjectId(
                projectId = projectId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }
        return projectTasks.filter { it.id in assignedIds }
    }
    fun insertComment(
        taskId: Long,
        content: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = SessionManager.userId

                val comment = CommentRequest(
                    task_id = taskId,
                    user_id = userId,
                    content = content
                )

                SupabaseApi.service.insertComment(comment)

                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Erro ao guardar comentário")
            }
        }

    }
}