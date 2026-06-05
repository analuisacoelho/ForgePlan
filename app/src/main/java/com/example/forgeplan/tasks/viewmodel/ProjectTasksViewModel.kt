package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ProjectTasksViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val project = MutableStateFlow<Project?>(null)
    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    fun loadProjectTasks(projectId: Long) {
        isLoading.value = true

        repository.getTasksByProjectId(
            projectId = projectId,
            onSuccess = {
                tasks.value = it
                isLoading.value = false
            },
            onError = {
                error.value = it
                isLoading.value = false
            }
        )
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {

            repository.updateTask(
                task = task,
                onSuccess = {
                    // 🔥 usa project_id (não projectId)
                    loadProjectTasks(task.project_id)
                },
                onError = { err ->
                    error.value = err
                }
            )
        }
    }
}