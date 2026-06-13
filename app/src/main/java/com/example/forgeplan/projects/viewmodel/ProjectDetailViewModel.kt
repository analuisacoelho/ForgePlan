package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.repository.ActivityLogRepository
import com.example.forgeplan.core.repository.ProjectRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectDetailViewModel : ViewModel() {

    private val repository = ProjectRepository()
    private val logRepository = ActivityLogRepository()

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadProject(projectId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getProjectById(
            projectId = projectId,
            onSuccess = { result ->
                _project.value = result
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    /**
     * Arquiva (soft-delete) o projeto. projectId pode ser local ou remoto.
     */
    fun archiveProject(
        projectId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.archiveProject(
            projectId = projectId,
            onSuccess = {
                viewModelScope.launch {
                    val pName = repository.getProjectNameById(projectId)
                    logRepository.logActivity(
                        action = "Archived project",
                        entityType = "project",
                        entityId = projectId,
                        detailsEn = "Admin: ${SessionManager.currentUser?.name} archived project '$pName'",
                        detailsPt = "Admin: ${SessionManager.currentUser?.name} arquivou o projeto '$pName'"
                    )
                }
                onSuccess()
            },
            onError = { message -> onError(message) }
        )
    }

    fun completeProject(
        projectId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val current = _project.value ?: return
        val payload = ProjectPayload(
            created_by_id = current.created_by_id,
            manager_id = current.manager_id,
            name = current.name,
            description = current.description,
            priority = current.priority,
            status = "DONE",
            start_date = current.start_date,
            end_date = current.end_date
        )
        repository.updateProject(
            projectId = projectId,
            project = payload,
            onSuccess = {
                _project.value = current.copy(status = "DONE")
                
                viewModelScope.launch {
                    val pName = current.name
                    logRepository.logActivity(
                        action = "Completed project",
                        entityType = "project",
                        entityId = projectId,
                        detailsEn = "Admin: ${SessionManager.currentUser?.name} marked project '$pName' as completed",
                        detailsPt = "Admin: ${SessionManager.currentUser?.name} marcou o projeto '$pName' como concluído"
                    )
                }
                
                onSuccess()
            },
            onError = { message -> onError(message) }
        )
    }
}
