package com.example.forgeplan.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.ForgeSectionTitle

@Composable
fun TimelineScreen(
    onProjectsClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

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
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "Search your task"
            )

            Spacer(modifier = Modifier.height(18.dp))

            ForgeSectionTitle(text = "Timeline")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineToggle(text = "Week", selected = true)
                TimelineToggle(text = "Month", selected = false)
            }

            Spacer(modifier = Modifier.height(14.dp))

            TimelineBoard()

            Spacer(modifier = Modifier.height(14.dp))

            TimelineSummary()
        }

        ForgePlanBottomBar(
            selectedItem = "Timeline",
            onProjectsClick = onProjectsClick,
            onProgressClick = onProgressClick,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun TimelineToggle(
    text: String,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TimelineBoard() {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(285.dp)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.width(95.dp)
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(30.dp))

                TimelineTaskLabel("Task 1")
                TimelineTaskLabel("Task 2")
                TimelineTaskLabel("Task 3")
                TimelineTaskLabel("Task 4")
                TimelineTaskLabel("Task 5")
            }

            TimelineDayColumn(
                day = "Monday",
                date = "15th Jan",
                bars = listOf(
                    TimelineBar("100% Finished", 120, 0),
                    TimelineBar("73% Active", 160, 1)
                )
            )

            TimelineDayColumn(
                day = "Tuesday",
                date = "16th Jan",
                bars = listOf(
                    TimelineBar("73% Active", 155, 1)
                )
            )

            TimelineDayColumn(
                day = "Wednesday",
                date = "17th Jan",
                bars = listOf(
                    TimelineBar("0% Pending", 170, 2)
                )
            )

            TimelineDayColumn(
                day = "Thursday",
                date = "18th Jan",
                bars = emptyList()
            )

            TimelineDayColumn(
                day = "Friday",
                date = "19th Jan",
                bars = listOf(
                    TimelineBar("0% Pending", 165, 2)
                )
            )
        }
    }
}

@Composable
fun TimelineDayColumn(
    day: String,
    date: String,
    bars: List<TimelineBar>
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(22.dp))

        bars.forEach { bar ->
            Spacer(modifier = Modifier.height((bar.verticalOffset * 42).dp))

            TimelineProgressBar(
                text = bar.text,
                width = bar.width
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TimelineProgressBar(
    text: String,
    width: Int
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}

@Composable
fun TimelineTaskLabel(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun TimelineSummary() {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "Summary of the day",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TimelineSummaryItem(number = "10", label = "Finished")
                TimelineSummaryItem(number = "3", label = "Active")
                TimelineSummaryItem(number = "13", label = "Pending")
            }
        }
    }
}

@Composable
fun TimelineSummaryItem(
    number: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

data class TimelineBar(
    val text: String,
    val width: Int,
    val verticalOffset: Int
)