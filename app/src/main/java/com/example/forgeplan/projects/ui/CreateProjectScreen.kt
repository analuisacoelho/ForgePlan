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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun CreateProjectScreen(
    onProjectCreated: () -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    val error by viewModel.error.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "New Project",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            ForgeSectionTitle(text = "Create project")

            Spacer(modifier = Modifier.height(14.dp))

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
                        placeholder = { Text("Name your project") },
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
                        placeholder = { Text("Describe the project") },
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
                        ProjectPriorityChip(
                            text = "LOW",
                            selected = priority == "LOW",
                            onClick = { priority = "LOW" }
                        )

                        ProjectPriorityChip(
                            text = "MEDIUM",
                            selected = priority == "MEDIUM",
                            onClick = { priority = "MEDIUM" }
                        )

                        ProjectPriorityChip(
                            text = "HIGH",
                            selected = priority == "HIGH",
                            onClick = { priority = "HIGH" }
                        )
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
                    onClick = onProjectCreated
                )

                ForgePrimaryButton(
                    text = "Save project",
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
                            val project = ProjectPayload(
                                created_by_id = null,
                                manager_id = null,
                                name = name.trim(),
                                description = description.trim().ifBlank { null },
                                priority = priority,
                                status = "IN_PROGRESS",
                                start_date = startDate.trim().ifBlank { null },
                                end_date = endDate.trim().ifBlank { null }
                            )

                            viewModel.createProject(
                                project = project,
                                onSuccess = onProjectCreated
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
fun ProjectPriorityChip(
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