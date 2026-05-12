package com.example.forgeplan.projects.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

@Composable
fun ManagerDashboardScreen(
    onProjectClick: (Long) -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Projetos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            error != null -> {
                Text(
                    text = error ?: "Erro desconhecido",
                    color = MaterialTheme.colorScheme.error
                )
            }

            projects.isEmpty() -> {
                Text(text = "Ainda não existem projetos.")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { project ->
                        ProjectCard(
                            project = project,
                            onClick = {
                                onProjectClick(project.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )

            project.description?.let { description ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Estado: ${project.status ?: "Sem estado"}")
            Text(text = "Prioridade: ${project.priority ?: "Sem prioridade"}")
            Text(text = "Fim: ${project.end_date ?: "Sem data"}")
        }
    }
}