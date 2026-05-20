package com.example.forgeplan.projects.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

@Composable
fun EditProjectScreen(
    projectId: Long,
    onProjectUpdated: () -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    val project by viewModel.selectedProject.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var status by remember { mutableStateOf("IN_PROGRESS") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadProjectById(projectId)
    }

    LaunchedEffect(project) {
        project?.let {
            name = it.name
            description = it.description ?: ""
            priority = it.priority ?: "MEDIUM"
            status = it.status ?: "IN_PROGRESS"
            startDate = it.start_date ?: ""
            endDate = it.end_date ?: ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "Edit Project",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            ForgeSectionTitle(text = "Edit project")

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading && project == null) {
                CircularProgressIndicator()
            } else {
                ForgeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = null
                            },
                            label = { Text("Project name") },
                            isError = nameError != null,
                            supportingText = {
                                nameError?.let { Text(it) }
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
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Dates",
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
                            ProjectEditChip("LOW", priority == "LOW") { priority = "LOW" }
                            ProjectEditChip("MEDIUM", priority == "MEDIUM") { priority = "MEDIUM" }
                            ProjectEditChip("HIGH", priority == "HIGH") { priority = "HIGH" }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProjectEditChip("IN_PROGRESS", status == "IN_PROGRESS") {
                                status = "IN_PROGRESS"
                            }

                            ProjectEditChip("DONE", status == "DONE") {
                                status = "DONE"
                            }
                        }

                        error?.let {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
                        onClick = onProjectUpdated
                    )

                    ForgePrimaryButton(
                        text = "Save changes",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            var hasError = false

                            if (name.isBlank()) {
                                nameError = "O nome do projeto é obrigatório."
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
                                val payload = ProjectPayload(
                                    created_by_id = project?.created_by_id,
                                    manager_id = project?.manager_id,
                                    name = name.trim(),
                                    description = description.trim().ifBlank { null },
                                    priority = priority,
                                    status = status,
                                    start_date = startDate.trim().ifBlank { null },
                                    end_date = endDate.trim().ifBlank { null }
                                )

                                viewModel.updateProject(
                                    projectId = projectId,
                                    project = payload,
                                    onSuccess = onProjectUpdated
                                )
                            }
                        }
                    )
                }
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Projects"
        )
    }
}

@Composable
fun ProjectEditChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}