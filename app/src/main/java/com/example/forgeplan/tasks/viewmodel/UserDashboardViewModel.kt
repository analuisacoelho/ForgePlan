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

/**
 * Lógica:
 *  1. Vai à tabela project_users e filtra por user_id → obtém os IDs dos projectos
 *  2. Para cada project_id vai buscar o Project completo
 *  3. Para cada projecto, vai à tabela task_assignments filtrando por user_id → obtém task_ids
 *  4. Para cada task_id vai buscar a Task completa
 *  5. Expõe projectsWithTasks: Map<Project, List<Task>>
 */
class UserDashboardViewModel : ViewModel() {

    private val projectUserRepo    = ProjectUserRepository()
    private val projectRepo        = ProjectRepository()
    private val taskAssignmentRepo = TaskAssignmentRepository()
    private val taskRepo           = TaskRepository()

    // Estado de carregamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Erro
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Projectos do utilizador com as suas tarefas
    private val _projectsWithTasks = MutableStateFlow<Map<Project, List<Task>>>(emptyMap())
    val projectsWithTasks: StateFlow<Map<Project, List<Task>>> = _projectsWithTasks

    // ── Entrada pública ──────────────────────────────────────────────────────

    fun loadDashboard() {
        val userId = SessionManager.userId
        if (userId == -1L) {
            _error.value = "Sessão inválida. Faz login novamente."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. Obtém IDs dos projectos onde o user participa
                val projectIds = fetchProjectIds(userId)

                // 2. Vai buscar cada projecto em paralelo
                val projects = projectIds.mapNotNull { fetchProject(it) }

                // 3. Para cada projecto, obtém as tarefas do user nesse projecto
                val result = linkedMapOf<Project, List<Task>>()
                for (project in projects) {
                    val tasks = fetchUserTasksForProject(userId, project.id)
                    result[project] = tasks
                }

                _projectsWithTasks.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Marca uma tarefa como concluída e recarrega o dashboard.
     */
    fun markTaskAsDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(status = "Done", completion_rate = 100)
            suspendCancellableCoroutine { cont ->
                taskRepo.updateTask(
                    task = updated,
                    onSuccess = { cont.resume(Unit) },
                    onError   = { cont.resume(Unit) }   // falha silenciosa, recarrega à mesma
                )
            }
            loadDashboard()   // recarrega para reflectir a alteração
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
        // Passo A – todos os task_ids atribuídos ao user
        val assignedTaskIds: List<Long> = suspendCancellableCoroutine { cont ->
            taskAssignmentRepo.getTaskIdsByUserId(
                userId    = userId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }
        if (assignedTaskIds.isEmpty()) return emptyList()

        // Passo B – tarefas deste projecto
        val projectTasks: List<Task> = suspendCancellableCoroutine { cont ->
            taskRepo.getTasksByProjectId(
                projectId = projectId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }

        // Passo C – intersecção: só as tarefas do projecto que foram atribuídas ao user
        return projectTasks.filter { it.id in assignedTaskIds }
    }
}