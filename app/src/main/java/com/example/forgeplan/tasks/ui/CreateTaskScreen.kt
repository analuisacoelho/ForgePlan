package com.example.forgeplan.tasks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
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
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "New Task",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            ForgeSectionTitle(text = "Create task")

            Spacer(modifier = Modifier.height(14.dp))

            TaskFormCard {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    label = { Text("Task name") },
                    placeholder = { Text("Name your task") },
                    isError = titleError != null,
                    supportingText = {
                        titleError?.let { Text(it) }
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe the task and all it needs") },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Start / End",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            dateError = null
                        },
                        label = { Text("Start") },
                        placeholder = { Text("YYYY-MM-DD") },
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = endDate,
                        onValueChange = {
                            endDate = it
                            dateError = null
                        },
                        label = { Text("End") },
                        placeholder = { Text("YYYY-MM-DD") },
                        isError = dateError != null,
                        supportingText = {
                            dateError?.let { Text(it) }
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityChip(
                        text = "LOW",
                        selected = priority == "LOW",
                        onClick = { priority = "LOW" }
                    )

                    PriorityChip(
                        text = "MEDIUM",
                        selected = priority == "MEDIUM",
                        onClick = { priority = "MEDIUM" }
                    )

                    PriorityChip(
                        text = "HIGH",
                        selected = priority == "HIGH",
                        onClick = { priority = "HIGH" }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Attachments",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttachmentPlaceholder(
                        text = "Upload Doc",
                        modifier = Modifier.weight(1f)
                    )

                    AttachmentPlaceholder(
                        text = "Take Photo",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ForgeSecondaryButton(
                    text = "Cancel",
                    modifier = Modifier.weight(1f),
                    onClick = onTaskCreated
                )

                ForgePrimaryButton(
                    text = "Save task",
                    modifier = Modifier.weight(1f),
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
                                priority = priority,
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
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        ForgePlanBottomBar(
            selectedItem = "Projects"
        )
    }
}

@Composable
fun TaskFormCard(
    content: @Composable ColumnScope.() -> Unit
) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun PriorityChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
fun AttachmentPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}