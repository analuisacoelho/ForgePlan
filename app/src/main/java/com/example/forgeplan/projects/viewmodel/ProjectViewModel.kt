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

class ProjectViewModel : ViewModel() {

    private val repository = ProjectRepository()
    private val logRepository = ActivityLogRepository()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _archivedProjects = MutableStateFlow<List<Project>>(emptyList())
    val archivedProjects: StateFlow<List<Project>> = _archivedProjects

    private val _isLoadingArchived = MutableStateFlow(false)
    val isLoadingArchived: StateFlow<Boolean> = _isLoadingArchived

    fun loadProjects() {
        _isLoading.value = true
        _error.value = null
        repository.getProjects(
            onSuccess = { projectList ->
                _projects.value = projectList
                _isLoading.value = false
            },
            onError = { errorMessage ->
                _error.value = errorMessage
                _isLoading.value = false
            }
        )
    }

    fun loadProjectById(projectId: Long) {
        _isLoading.value = true
        _error.value = null
        repository.getProjectById(
            projectId = projectId,
            onSuccess = { project ->
                _selectedProject.value = project
                _isLoading.value = false
            },
            onError = { errorMessage ->
                _error.value = errorMessage
                _isLoading.value = false
            }
        )
    }

    fun createProject(
        project: ProjectPayload,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null
        repository.createProject(
            project = project,
            onSuccess = { created ->
                _isLoading.value = false
                loadProjects()
                
                created?.let {
                    logRepository.logActivity(
                        action = "Created project",
                        entityType = "project",
                        entityId = it.id,
                        detailsEn = "Admin: ${SessionManager.currentUser?.name} created project '${it.name}'",
                        detailsPt = "Admin: ${SessionManager.currentUser?.name} criou o projeto '${it.name}'"
                    )
                }
                
                onSuccess()
            },
            onError = { errorMessage ->
                _error.value = errorMessage
                _isLoading.value = false
            }
        )
    }

    fun updateProject(
        projectId: Long,
        project: ProjectPayload,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null
        repository.updateProject(
            projectId = projectId,
            project = project,
            onSuccess = {
                _isLoading.value = false
                loadProjects()
                
                logRepository.logActivity(
                    action = "Updated project",
                    entityType = "project",
                    entityId = projectId,
                    detailsEn = "Admin: ${SessionManager.currentUser?.name} updated project '${project.name}'",
                    detailsPt = "Admin: ${SessionManager.currentUser?.name} atualizou o projeto '${project.name}'"
                )

                onSuccess()
            },
            onError = { errorMessage ->
                _error.value = errorMessage
                _isLoading.value = false
            }
        )
    }

    fun loadArchivedProjects() {
        _isLoadingArchived.value = true
        repository.getArchivedProjects(
            onSuccess = { projects ->
                _archivedProjects.value = projects
                _isLoadingArchived.value = false
            },
            onError = {
                _archivedProjects.value = emptyList()
                _isLoadingArchived.value = false
            }
        )
    }

    fun restoreProject(projectId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        repository.restoreProject(
            projectId = projectId,
            onSuccess = {
                _archivedProjects.value = _archivedProjects.value.filter { it.id != projectId }
                loadProjects()
                loadArchivedProjects()

                viewModelScope.launch {
                    val pName = repository.getProjectNameById(projectId)
                    logRepository.logActivity(
                        action = "Restored project",
                        entityType = "project",
                        entityId = projectId,
                        detailsEn = "Admin: ${SessionManager.currentUser?.name} restored project '$pName'",
                        detailsPt = "Admin: ${SessionManager.currentUser?.name} restaurou o projeto '$pName'"
                    )
                }

                onSuccess()
            },
            onError = onError
        )
    }
}