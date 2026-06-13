package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.TaskGroup
import com.example.forgeplan.core.repository.ActivityLogRepository
import com.example.forgeplan.core.repository.ProjectRepository
import com.example.forgeplan.core.repository.TaskGroupRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskGroupViewModel : ViewModel() {

    private val repository = TaskGroupRepository()
    private val projectRepo = ProjectRepository()
    private val logRepository = ActivityLogRepository()

    private val _groups = MutableStateFlow<List<TaskGroup>>(emptyList())
    val groups: StateFlow<List<TaskGroup>> = _groups

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadGroups(projectId: Long) {
        _error.value = null

        repository.getGroupsByProjectId(
            projectId = projectId,
            onSuccess = { groupList ->
                _groups.value = groupList
            },
            onError = { message ->
                _error.value = message
            }
        )
    }

    fun createGroup(
        projectId: Long,
        name: String,
        onSuccess: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            _error.value = "O nome do grupo é obrigatório."
            return
        }

        _error.value = null

        repository.createGroup(
            projectId = projectId,
            name = name.trim(),
            onSuccess = {
                loadGroups(projectId)

                viewModelScope.launch {
                    val pName = projectRepo.getProjectNameById(projectId)
                    logRepository.logActivity(
                        action = "Created task group",
                        entityType = "task_group",
                        entityId = projectId,
                        detailsEn = "Manager: ${SessionManager.currentUser?.name} created task group '$name' in project '$pName'",
                        detailsPt = "Manager: ${SessionManager.currentUser?.name} criou o grupo de tarefas '$name' no projeto '$pName'"
                    )
                }

                onSuccess()
            },
            onError = { message ->
                _error.value = message
            }
        )
    }
}