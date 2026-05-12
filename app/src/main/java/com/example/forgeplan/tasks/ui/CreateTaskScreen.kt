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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

@Composable
fun CreateTaskScreen(
    projectId: Long,
    onTaskCreated: () -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Criar Tarefa",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = {
                title = it
                titleError = null
            },
            label = { Text("Título") },
            isError = titleError != null,
            supportingText = {
                titleError?.let { Text(it) }
            }
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
            value = priority,
            onValueChange = { priority = it.uppercase() },
            label = { Text("Prioridade") },
            supportingText = {
                Text("Usa LOW, MEDIUM ou HIGH")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = startDate,
            onValueChange = {
                startDate = it
                dateError = null
            },
            label = { Text("Data de início") },
            placeholder = { Text("YYYY-MM-DD") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = endDate,
            onValueChange = {
                endDate = it
                dateError = null
            },
            label = { Text("Data de fim") },
            placeholder = { Text("YYYY-MM-DD") },
            isError = dateError != null,
            supportingText = {
                dateError?.let { Text(it) }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                var hasError = false

                if (title.isBlank()) {
                    titleError = "O título é obrigatório."
                    hasError = true
                }

                if (
                    startDate.isNotBlank() &&
                    endDate.isNotBlank() &&
                    endDate < startDate
                ) {
                    dateError = "A data de fim não pode ser anterior à data de início."
                    hasError = true
                }

                if (!hasError) {
                    val task = Task(
                        id = 0,
                        project_id = projectId,
                        created_by_id = null,
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        status = "PENDING",
                        priority = priority.trim().ifBlank { "MEDIUM" },
                        completion_rate = 0,
                        start_date = startDate.trim().ifBlank { null },
                        end_date = endDate.trim().ifBlank { null }
                    )

                    viewModel.createTask(
                        task = task,
                        onSuccess = onTaskCreated
                    )
                }
            }
        ) {
            Text("Guardar tarefa")
        }
    }
}