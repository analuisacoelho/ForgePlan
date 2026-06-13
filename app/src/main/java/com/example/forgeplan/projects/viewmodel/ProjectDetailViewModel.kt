package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProjectDetailViewModel : ViewModel() {

    private val repository = ProjectRepository()

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
            onSuccess = { onSuccess() },
            onError = { message -> onError(message) }
        )
    }
}