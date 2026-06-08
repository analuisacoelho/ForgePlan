package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.repository.TaskAssignmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskAssignmentViewModel : ViewModel() {

    private val repository = TaskAssignmentRepository()

    private val _assignments = MutableStateFlow<List<TaskAssignment>>(emptyList())
    val assignments: StateFlow<List<TaskAssignment>> = _assignments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadAssignments(taskId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getAssignmentsByTaskId(
            taskId = taskId,
            onSuccess = { assignmentList ->
                _assignments.value = assignmentList
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun assignUserToTask(
        taskId: Long,
        userId: Long,
        onSuccess: () -> Unit = {}
    ) {
        _isLoading.value = true
        _error.value = null

        val assignment = TaskAssignment(
            task_id = taskId,
            user_id = userId,
            assigned_at = null
        )

        repository.assignUserToTask(
            assignment = assignment,
            onSuccess = {
                loadAssignments(taskId)
                _isLoading.value = false
                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun removeUserFromTask(
        taskId: Long,
        userId: Long,
        onSuccess: () -> Unit = {}
    ) {
        _isLoading.value = true
        _error.value = null

        repository.removeUserFromTask(
            taskId = taskId,
            userId = userId,
            onSuccess = {
                loadAssignments(taskId)
                _isLoading.value = false
                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun toggleUserAssignment(
        taskId: Long,
        userId: Long,
        isCurrentlyAssigned: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        if (isCurrentlyAssigned) {
            removeUserFromTask(
                taskId = taskId,
                userId = userId,
                onSuccess = onSuccess
            )
        } else {
            assignUserToTask(
                taskId = taskId,
                userId = userId,
                onSuccess = onSuccess
            )
        }
    }
}