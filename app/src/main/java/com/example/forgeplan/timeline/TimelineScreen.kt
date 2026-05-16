package com.example.forgeplan.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.core.ui.components.StatusChip

@Composable
fun TimelineScreen(
    onProjectsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            ForgeSearchBar(
                value = "",
                onValueChange = {},
                placeholder = "Search your task"
            )

            Spacer(modifier = Modifier.height(18.dp))

            ForgeSectionTitle(text = "Timeline")

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TimelineColumn(
                    title = "Backlog",
                    count = 4,
                    tasks = listOf(
                        TimelineTask("Definir precedências", "Pending", "HIGH"),
                        TimelineTask("Exportar estatísticas", "Pending", "MEDIUM"),
                        TimelineTask("Timeline visual", "Pending", "HIGH"),
                        TimelineTask("Avaliação performance", "Pending", "LOW")
                    )
                )

                TimelineColumn(
                    title = "To Do",
                    count = 3,
                    tasks = listOf(
                        TimelineTask("Login com validação", "To do", "HIGH"),
                        TimelineTask("Autenticação JWT", "To do", "HIGH"),
                        TimelineTask("Criar/editar projeto", "To do", "MEDIUM")
                    )
                )

                TimelineColumn(
                    title = "In Progress",
                    count = 2,
                    tasks = listOf(
                        TimelineTask("Criar/editar tarefas", "Active", "HIGH"),
                        TimelineTask("Associar utilizadores", "Active", "MEDIUM")
                    )
                )

                TimelineColumn(
                    title = "Review",
                    count = 2,
                    tasks = listOf(
                        TimelineTask("Lista de projetos", "Review", "LOW"),
                        TimelineTask("Tarefas concluídas", "Review", "MEDIUM")
                    )
                )

                TimelineColumn(
                    title = "Done",
                    count = 5,
                    tasks = listOf(
                        TimelineTask("Supabase + tabelas", "Done", "BASE"),
                        TimelineTask("Data classes Kotlin", "Done", "BASE"),
                        TimelineTask("Navigation Component", "Done", "BASE"),
                        TimelineTask("Retrofit + Repository", "Done", "BASE"),
                        TimelineTask("Room + entidades", "Done", "BASE")
                    )
                )
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Timeline",
            onProjectsClick = onProjectsClick
        )
    }
}

@Composable
fun TimelineColumn(
    title: String,
    count: Int,
    tasks: List<TimelineTask>
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        tasks.forEach { task ->
            TimelineTaskCard(task = task)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun TimelineTaskCard(
    task: TimelineTask
) {
    ForgeCard {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            StatusChip(text = task.priority)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.status,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

data class TimelineTask(
    val title: String,
    val status: String,
    val priority: String
)