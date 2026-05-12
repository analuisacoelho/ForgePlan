package com.example.forgeplan.tasks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.tasks.viewmodel.TaskAssignmentViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

@Composable
fun EditTaskScreen(
    taskId: Long,
    onTaskUpdated: () -> Unit,
    viewModel: TaskViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    assignmentViewModel: TaskAssignmentViewModel = viewModel()
) {
    val selectedTask by viewModel.selectedTask.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val users by userViewModel.users.collectAsState()
    val assignmentError by assignmentViewModel.error.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var completionRate by remember { mutableStateOf("") }

    var assignmentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskById(taskId)
        userViewModel.loadUsers()
    }

    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            title = task.title
            description = task.description ?: ""
            status = task.status ?: ""
            priority = task.priority ?: ""
            completionRate = (task.completion_rate ?: 0).toString()
        }
    }

    if (isLoading && selectedTask == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            CircularProgressIndicator()
        }

        return
    }

    selectedTask?.let { task ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Editar Tarefa",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = status,
                onValueChange = { status = it.uppercase() },
                label = { Text("Estado") },
                supportingText = {
                    Text("PENDING, IN_PROGRESS ou DONE")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = priority,
                onValueChange = { priority = it.uppercase() },
                label = { Text("Prioridade") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = completionRate,
                onValueChange = { completionRate = it },
                label = { Text("Conclusão (%)") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val updatedTask = Task(
                        id = task.id,
                        project_id = task.project_id,
                        created_by_id = task.created_by_id,
                        title = title,
                        description = description,
                        status = status,
                        priority = priority,
                        completion_rate = completionRate.toIntOrNull() ?: 0,
                        start_date = task.start_date,
                        end_date = task.end_date
                    )

                    viewModel.updateTask(
                        task = updatedTask,
                        onSuccess = onTaskUpdated
                    )
                }
            ) {
                Text("Guardar alterações")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Associar utilizador à tarefa",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            assignmentMessage?.let {
                Text(text = it)
                Spacer(modifier = Modifier.height(8.dp))
            }

            assignmentError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (users.isEmpty()) {
                Text(text = "Não existem utilizadores disponíveis.")
            } else {
                users.forEach { user ->
                    UserAssignmentCard(
                        user = user,
                        onAssignClick = {
                            assignmentViewModel.assignUserToTask(
                                taskId = task.id,
                                userId = user.id,
                                onSuccess = {
                                    assignmentMessage =
                                        "Utilizador ${user.name} associado à tarefa."
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UserAssignmentCard(
    user: User,
    onAssignClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleSmall
            )

            Text(text = "@${user.username}")
            Text(text = user.email)

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAssignClick
            ) {
                Text(text = "Associar")
            }
        }
    }
}