package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.repository.ProjectUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProjectUserViewModel : ViewModel() {

    private val repository = ProjectUserRepository()

    private val _projectUsers = MutableStateFlow<List<ProjectUser>>(emptyList())
    val projectUsers: StateFlow<List<ProjectUser>> = _projectUsers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadProjectUsers(projectId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getProjectUsersByProjectId(
            projectId = projectId,
            onSuccess = { users ->
                _projectUsers.value = users
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun assignUserToProject(
        projectId: Long,
        userId: Long,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null

        val projectUser = ProjectUser(
            project_user_id = 0,
            project_id = projectId,
            user_id = userId,
            joined_at = null
        )

        repository.assignUserToProject(
            projectUser = projectUser,
            onSuccess = {
                loadProjectUsers(projectId)
                _isLoading.value = false
                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }
}