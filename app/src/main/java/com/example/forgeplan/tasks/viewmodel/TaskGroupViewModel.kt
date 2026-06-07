package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.TaskGroup
import com.example.forgeplan.core.repository.TaskGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskGroupViewModel : ViewModel() {

    private val repository = TaskGroupRepository()

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
                onSuccess()
            },
            onError = { message ->
                _error.value = message
            }
        )
    }
}