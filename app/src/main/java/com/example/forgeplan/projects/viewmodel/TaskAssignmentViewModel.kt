package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.repository.TaskAssignmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskAssignmentViewModel : ViewModel() {

    private val repository = TaskAssignmentRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun assignUserToTask(
        taskId: Long,
        userId: Long,
        onSuccess: () -> Unit
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