package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProjectViewModel : ViewModel() {

    private val repository = ProjectRepository()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
}