package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.ProjectEvaluation
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.core.repository.ProjectEvaluationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProjectEvaluationViewModel : ViewModel() {

    private val repository = ProjectEvaluationRepository()

    private val _evaluations = MutableStateFlow<List<ProjectEvaluation>>(emptyList())
    val evaluations: StateFlow<List<ProjectEvaluation>> = _evaluations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadEvaluations(
        projectId: Long,
        onFinished: (() -> Unit)? = null
    ) {
        _isLoading.value = true
        _error.value = null

        repository.getEvaluations(
            projectId = projectId,
            onSuccess = { evaluations ->
                _evaluations.value = evaluations
                _isLoading.value = false
                onFinished?.invoke()
            },
            onError = { errorMessage ->
                _error.value = errorMessage
                _isLoading.value = false
                onFinished?.invoke()
            }
        )
    }

    fun createEvaluation(
        evaluation: ProjectEvaluationPayload,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null

        repository.createEvaluation(
            evaluation = evaluation,
            onSuccess = { createdEvaluation ->
                val currentList = _evaluations.value.toMutableList()

                createdEvaluation?.let {
                    currentList.add(0, it)
                    _evaluations.value = currentList
                }

                loadEvaluations(
                    projectId = evaluation.project_id,
                    onFinished = {
                        _isLoading.value = false
                        onSuccess()
                    }
                )
            },
            onError = { errorMessage ->
                _error.value = errorMessage
                _isLoading.value = false
            }
        )
    }

    fun hasEvaluation(): Boolean {
        return _evaluations.value.isNotEmpty()
    }

    fun latestEvaluation(): ProjectEvaluation? {
        return _evaluations.value.firstOrNull()
    }
}