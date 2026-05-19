package com.example.forgeplan.team.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.core.ui.components.StatusChip
import com.example.forgeplan.core.ui.components.UserAvatarChip

@Composable
fun TeamScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {}
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
                placeholder = "Search team member"
            )

            Spacer(modifier = Modifier.height(18.dp))

            ForgeSectionTitle(text = "Your Team")

            Spacer(modifier = Modifier.height(14.dp))

            TeamMemberCard(
                initials = "A",
                name = "Administrador",
                username = "@admin",
                email = "admin@forgeplan.pt",
                role = "Admin",
                tasks = "5 tasks"
            )

            Spacer(modifier = Modifier.height(10.dp))

            TeamMemberCard(
                initials = "GP",
                name = "Gestor Projeto",
                username = "@gestor",
                email = "gestor@forgeplan.pt",
                role = "Manager",
                tasks = "3 tasks"
            )

            Spacer(modifier = Modifier.height(10.dp))

            TeamMemberCard(
                initials = "A",
                name = "Ana",
                username = "@ana",
                email = "ana@forgeplan.pt",
                role = "Worker",
                tasks = "4 tasks"
            )

            Spacer(modifier = Modifier.height(10.dp))

            TeamMemberCard(
                initials = "T",
                name = "Tiago",
                username = "@tiago",
                email = "tiago@forgeplan.pt",
                role = "Worker",
                tasks = "2 tasks"
            )
        }

        ForgePlanBottomBar(
            selectedItem = "Team",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onProgressClick = onProgressClick
        )
    }
}

@Composable
fun TeamMemberCard(
    initials: String,
    name: String,
    username: String,
    email: String,
    role: String,
    tasks: String
) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatarChip(initials = initials)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = username,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(text = role)
                    StatusChip(text = tasks)
                }
            }
        }
    }
}