package com.example.forgeplan.projects.ui

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import java.util.Calendar

@Composable
fun EditProjectScreen(
    projectId: Long,
    onProjectUpdated: () -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val project by viewModel.selectedProject.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadProjectById(projectId)
        // corre uma vez quando o ecrã abre, para carregar o projeto a editar
    }

    LaunchedEffect(project) {
        project?.let {
            name = it.name
            // preenche os campos do formulário quando o projeto chega do ViewModel
            // separado do LaunchedEffect anterior porque o projeto chega de forma assíncrona
            description = it.description ?: ""
            priority = it.priority ?: "MEDIUM"
            startDate = it.start_date ?: ""
            endDate = it.end_date ?: ""
        }
    }

    fun saveProject() {
        var hasError = false
        val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
        // valida formato YYYY-MM-DD antes de enviar para a API

        if (name.isBlank()) {
            nameError = appText(
                en = "Project name is required.",
                pt = "O nome do projeto é obrigatório."
            )
            hasError = true
        }

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
            val payload = ProjectPayload(
                created_by_id = project?.created_by_id,
                manager_id = project?.manager_id,
                name = name.trim(),
                description = description.trim().ifBlank { null },
                priority = priority,
                status = project?.status ?: "IN_PROGRESS",  // mantém o status atual do projeto
                start_date = startDate.trim().ifBlank { null },
                end_date = endDate.trim().ifBlank { null }   // descrição vazia enviada como null,
            )

            viewModel.updateProject(
                projectId = projectId,
                project = payload,
                onSuccess = onProjectUpdated
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = appText(en = "Edit Project", pt = "Editar Projeto"),
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
                text = appText(en = "Edit project", pt = "Editar projeto"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 14.dp))

            if (isLoading && project == null) {
                CircularProgressIndicator()
            } else {
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            EditProjectMainFields(
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
                            EditProjectDatePriorityFields(
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

                            EditProjectErrorMessage(error = error)

                            Spacer(modifier = Modifier.height(18.dp))

                            EditProjectButtons(
                                onCancel = onProjectUpdated,
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
                            EditProjectMainFields(
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

                            EditProjectDatePriorityFields(
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

                            EditProjectErrorMessage(error = error)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    EditProjectButtons(
                        onCancel = onProjectUpdated,
                        onSave = { saveProject() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Projects"
        )
    }
}

@Composable
fun EditProjectMainFields(
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
fun EditProjectDatePriorityFields(
    startDate: String,
    endDate: String,
    dateError: String?,
    priority: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit
) {
    val context = LocalContext.current

    fun parseDateParts(date: String): Triple<Int, Int, Int> {
        // converte string "YYYY-MM-DD" para (ano, mês, dia)
        // mês é subtraído 1 porque Calendar usa meses 0-indexados
        val cal = Calendar.getInstance()
        return if (date.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            val parts = date.split("-")
            Triple(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        } else {
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)) // se não há data, abre no dia de hoje
        }
    }

    fun showDatePicker(currentDate: String, onDateSelected: (String) -> Unit) {
        val (year, month, day) = parseDateParts(currentDate)
        DatePickerDialog(context, { _, y, m, d ->
            onDateSelected("%04d-%02d-%02d".format(y, m + 1, d))
            // m + 1 porque DatePickerDialog também devolve meses 0-indexados
        }, year, month, day).show()
    }

    Text(
        text = appText(en = "Dates", pt = "Datas"),
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
            label = { Text(appText(en = "Start", pt = "Início")) },
            placeholder = { Text("YYYY-MM-DD") },
            shape = RoundedCornerShape(14.dp),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker(startDate) { onStartDateChange(it) } }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = endDate,
            onValueChange = onEndDateChange,
            label = { Text(appText(en = "End", pt = "Fim")) },
            placeholder = { Text("YYYY-MM-DD") },
            isError = dateError != null,
            supportingText = { dateError?.let { Text(it) } },
            shape = RoundedCornerShape(14.dp),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker(endDate) { onEndDateChange(it) } }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
        listOf(
            "LOW" to appText(en = "LOW", pt = "BAIXA"),
            "MEDIUM" to appText(en = "MEDIUM", pt = "MÉDIA"),
            "HIGH" to appText(en = "HIGH", pt = "ALTA")
        ).forEach { (value, label) ->
            ProjectEditChip(
                modifier = Modifier.weight(1f),
                text = label,
                selected = priority == value,
                onClick = { onPriorityChange(value) }
            )
        }
    }
}

@Composable
fun EditProjectErrorMessage(
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
fun EditProjectButtons(
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
                text = appText(en = "Save changes", pt = "Guardar alterações"),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProjectEditChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        // FilterChip = componente com estado visual de selecionado/não selecionado
        // usado aqui para escolha de prioridade (LOW / MEDIUM / HIGH)
        modifier = modifier,
        selected = selected, // muda visualmente consoante a prioridade ativa
        onClick = onClick,
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    )
}