package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.ProjectUserPayload
import com.example.forgeplan.core.repository.ActivityLogRepository
import com.example.forgeplan.core.repository.ProjectRepository
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.UserRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectUserViewModel : ViewModel() {

    private val repository = ProjectUserRepository()
    private val logRepository = ActivityLogRepository()
    private val userRepo = UserRepository()
    private val projectRepo = ProjectRepository()

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

        val projectUser = ProjectUserPayload(
            project_id = projectId,
            user_id = userId
        )

        repository.assignUserToProject(
            projectUser = projectUser,
            onSuccess = {
                loadProjectUsers(projectId)
                _isLoading.value = false
                
                viewModelScope.launch {
                    val uName = userRepo.getUserNameById(userId)
                    val pName = projectRepo.getProjectNameById(projectId)
                    logRepository.logActivity(
                        action = "Assigned user to project",
                        entityType = "project_user",
                        entityId = projectId,
                        detailsEn = "Manager: ${SessionManager.currentUser?.name} assigned user '$uName' to project '$pName'",
                        detailsPt = "Manager: ${SessionManager.currentUser?.name} associou o utilizador '$uName' ao projeto '$pName'"
                    )
                }

                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun removeUserFromProject(
        projectId: Long,
        userId: Long,
        onSuccess: () -> Unit = {}
    ) {
        _isLoading.value = true
        _error.value = null

        repository.removeUserFromProject(
            projectId = projectId,
            userId = userId,
            onSuccess = {
                loadProjectUsers(projectId)
                _isLoading.value = false

                viewModelScope.launch {
                    val uName = userRepo.getUserNameById(userId)
                    val pName = projectRepo.getProjectNameById(projectId)
                    logRepository.logActivity(
                        action = "Removed user from project",
                        entityType = "project_user",
                        entityId = projectId,
                        detailsEn = "Manager: ${SessionManager.currentUser?.name} removed user '$uName' from project '$pName'",
                        detailsPt = "Manager: ${SessionManager.currentUser?.name} removeu o utilizador '$uName' do projeto '$pName'"
                    )
                }

                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }
}