package com.example.forgeplan.projects.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.projects.viewmodel.ProjectDetailViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onCreateTaskClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    viewModel: ProjectDetailViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val project by viewModel.project.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()

    val pendingTasks = tasks.filter { it.status != "DONE" }
    val completedTasks = tasks.filter { it.status == "DONE" }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Detalhe do Projeto",
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

            project == null -> {
                Text(text = "Projeto não encontrado.")
            }

            else -> {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = project!!.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = project!!.description ?: "Sem descrição")
                        Text(text = "Estado: ${project!!.status ?: "Sem estado"}")
                        Text(text = "Prioridade: ${project!!.priority ?: "Sem prioridade"}")
                        Text(text = "Início: ${project!!.start_date ?: "Sem data"}")
                        Text(text = "Fim: ${project!!.end_date ?: "Sem data"}")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCreateTaskClick
                ) {
                    Text(text = "Nova tarefa")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tarefas Pendentes",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (pendingTasks.isEmpty()) {
                    Text(text = "Não existem tarefas pendentes.")
                } else {
                    LazyColumn {
                        items(pendingTasks) { task ->
                            TaskCard(
                                task = task,
                                onClick = {
                                    onTaskClick(task.id)
                                },
                                taskViewModel = taskViewModel
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tarefas Concluídas",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (completedTasks.isEmpty()) {
                    Text(text = "Ainda não existem tarefas concluídas.")
                } else {
                    LazyColumn {
                        items(completedTasks) { task ->
                            TaskCard(
                                task = task,
                                onClick = {
                                    onTaskClick(task.id)
                                },
                                taskViewModel = taskViewModel
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    taskViewModel: TaskViewModel
) {
    Card(
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = task.description ?: "Sem descrição")

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Estado: ${task.status ?: "Sem estado"}")
            Text(text = "Prioridade: ${task.priority ?: "Sem prioridade"}")
            Text(text = "Conclusão: ${task.completion_rate ?: 0}%")
            Text(text = "Fim: ${task.end_date ?: "Sem data"}")

            if (task.status != "DONE") {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val updatedTask = task.copy(
                            status = "DONE",
                            completion_rate = 100
                        )

                        taskViewModel.updateTask(
                            task = updatedTask,
                            onSuccess = {
                                taskViewModel.loadTasks(task.project_id)
                            }
                        )
                    }
                ) {
                    Text(text = "Concluir")
                }
            }
        }
    }
}