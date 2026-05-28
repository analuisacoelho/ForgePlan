package com.example.forgeplan.projects.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

@Composable
fun CreateProjectScreen(
    onProjectCreated: () -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val error by viewModel.error.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    fun saveProject() {
        var hasError = false

        if (name.isBlank()) {
            nameError = appText(
                en = "Project name is required.",
                pt = "O nome do projeto é obrigatório."
            )
            hasError = true
        }

        val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

        if (startDate.isNotBlank() && !dateRegex.matches(startDate)) {
            dateError = appText(
                en = "The start date must be in YYYY-MM-DD format.",
                pt = "A data de início deve estar no formato YYYY-MM-DD."
            )
            hasError = true
        }

        if (endDate.isNotBlank() && !dateRegex.matches(endDate)) {
            dateError = appText(
                en = "The end date must be in YYYY-MM-DD format.",
                pt = "A data de fim deve estar no formato YYYY-MM-DD."
            )
            hasError = true
        }

        if (
            startDate.isNotBlank() &&
            endDate.isNotBlank() &&
            endDate < startDate
        ) {
            dateError = appText(
                en = "The end date cannot be earlier than the start date.",
                pt = "A data de fim não pode ser anterior à data de início."
            )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = appText(en = "New Project", pt = "Novo Projeto"),
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 32.dp else 18.dp,
                    vertical = if (isLandscape) 14.dp else 16.dp
                )
        ) {
            Text(
                text = appText(en = "Create project", pt = "Criar projeto"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 18.dp))

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ProjectMainFields(
                            name = name,
                            description = description,
                            nameError = nameError,
                            onNameChange = {
                                name = it
                                nameError = null
                            },
                            onDescriptionChange = { description = it }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        ProjectDateAndPriorityFields(
                            startDate = startDate,
                            endDate = endDate,
                            dateError = dateError,
                            priority = priority,
                            onStartDateChange = {
                                startDate = it
                                dateError = null
                            },
                            onEndDateChange = {
                                endDate = it
                                dateError = null
                            },
                            onPriorityChange = { priority = it }
                        )

                        ProjectErrorMessage(error = error)

                        Spacer(modifier = Modifier.height(18.dp))

                        CreateProjectButtons(
                            onCancel = onProjectCreated,
                            onSave = { saveProject() }
                        )
                    }
                }
            } else {
                ForgeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        ProjectMainFields(
                            name = name,
                            description = description,
                            nameError = nameError,
                            onNameChange = {
                                name = it
                                nameError = null
                            },
                            onDescriptionChange = { description = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProjectDateAndPriorityFields(
                            startDate = startDate,
                            endDate = endDate,
                            dateError = dateError,
                            priority = priority,
                            onStartDateChange = {
                                startDate = it
                                dateError = null
                            },
                            onEndDateChange = {
                                endDate = it
                                dateError = null
                            },
                            onPriorityChange = { priority = it }
                        )

                        ProjectErrorMessage(error = error)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                CreateProjectButtons(
                    onCancel = onProjectCreated,
                    onSave = { saveProject() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Projects"
        )
    }
}

@Composable
fun ProjectMainFields(
    name: String,
    description: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = name,
        onValueChange = onNameChange,
        label = {
            Text(appText(en = "Project name", pt = "Nome do projeto"))
        },
        placeholder = {
            Text(appText(en = "Name your project", pt = "Nome do projeto"))
        },
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
        onValueChange = onDescriptionChange,
        label = {
            Text(appText(en = "Description", pt = "Descrição"))
        },
        placeholder = {
            Text(appText(en = "Describe the project", pt = "Descreve o projeto"))
        },
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun ProjectDateAndPriorityFields(
    startDate: String,
    endDate: String,
    dateError: String?,
    priority: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit
) {
    Text(
        text = appText(en = "Start / End", pt = "Início / Fim"),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = startDate,
            onValueChange = onStartDateChange,
            label = {
                Text(appText(en = "Start", pt = "Início"))
            },
            placeholder = { Text("YYYY-MM-DD") },
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = endDate,
            onValueChange = onEndDateChange,
            label = {
                Text(appText(en = "End", pt = "Fim"))
            },
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
        text = appText(en = "Priority", pt = "Prioridade"),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProjectPriorityChip(
            text = appText(en = "LOW", pt = "BAIXA"),
            selected = priority == "LOW",
            onClick = { onPriorityChange("LOW") }
        )

        ProjectPriorityChip(
            text = appText(en = "MEDIUM", pt = "MÉDIA"),
            selected = priority == "MEDIUM",
            onClick = { onPriorityChange("MEDIUM") }
        )

        ProjectPriorityChip(
            text = appText(en = "HIGH", pt = "ALTA"),
            selected = priority == "HIGH",
            onClick = { onPriorityChange("HIGH") }
        )
    }
}

@Composable
fun ProjectErrorMessage(
    error: String?
) {
    error?.let {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CreateProjectButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            onClick = onCancel,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = appText(en = "Cancel", pt = "Cancelar"),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            onClick = onSave,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = appText(en = "Save project", pt = "Guardar projeto"),
                fontWeight = FontWeight.Bold
            )
        }
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