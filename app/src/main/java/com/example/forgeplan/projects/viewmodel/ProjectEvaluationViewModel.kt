package com.example.forgeplan.projects.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.ProjectEvaluation
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.core.repository.ProjectEvaluationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProjectEvaluationViewModel : ViewModel() {

    private val repository = ProjectEvaluationRepository()

    private val _evaluations =
        MutableStateFlow<List<ProjectEvaluation>>(emptyList())

    val evaluations: StateFlow<List<ProjectEvaluation>>
        get() = _evaluations

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?>
        get() = _error

    fun loadEvaluations(projectId: Long) {
        repository.getEvaluations(
            projectId = projectId,
            onSuccess = {
                _evaluations.value = it
            },
            onError = {
                _error.value = it
            }
        )
    }

    fun createEvaluation(
        evaluation: ProjectEvaluationPayload,
        onSuccess: () -> Unit
    ) {
        repository.createEvaluation(
            evaluation = evaluation,
            onSuccess = {
                loadEvaluations(evaluation.project_id)
                onSuccess()
            },
            onError = {
                _error.value = it
            }
        )
    }
}