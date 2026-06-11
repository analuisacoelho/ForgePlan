package com.example.forgeplan.timeline.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.repository.ProjectRepository
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ViewModel para a Timeline do Manager.
 * Usa project_users para saber quais projetos o manager pertence,
 * igual ao ManagerDashboardScreen.
 */
class ManagerTimelineViewModel : ViewModel() {

    private val projectRepo     = ProjectRepository()
    private val projectUserRepo = ProjectUserRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    fun loadTimeline() {
        val managerId = SessionManager.userId
        if (managerId == -1L) {
            _error.value = "Sessão inválida. Faz login novamente."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 1. IDs dos projetos onde o manager é membro (project_users)
                val projectIds = fetchProjectIds(managerId)
                if (projectIds.isEmpty()) {
                    _projects.value = emptyList()
                    return@launch
                }

                // 2. Todos os projetos, filtrados pelos IDs acima
                val allProjects = fetchAllProjects()
                _projects.value = allProjects.filter { it.id in projectIds }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro desconhecido"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchProjectIds(userId: Long): List<Long> =
        suspendCancellableCoroutine { cont ->
            projectUserRepo.getProjectIdsByUserId(
                userId    = userId,
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }

    private suspend fun fetchAllProjects(): List<Project> =
        suspendCancellableCoroutine { cont ->
            projectRepo.getProjects(
                onSuccess = { cont.resume(it) },
                onError   = { cont.resume(emptyList()) }
            )
        }
}