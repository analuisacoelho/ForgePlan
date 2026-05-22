package com.example.forgeplan.team.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.core.ui.components.UserAvatarChip

data class TeamMemberUi(
    val initials: String,
    val name: String,
    val username: String,
    val email: String,
    val role: String,
    val status: String,
    val projects: String
)

@Composable
fun TeamScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

    val members = listOf(
        TeamMemberUi(
            initials = "A",
            name = "Administrador",
            username = "@admin",
            email = "admin@forgeplan.pt",
            role = "Admin",
            status = "Online",
            projects = "5 Projects"
        ),
        TeamMemberUi(
            initials = "GP",
            name = "Gestor Projeto",
            username = "@gestor",
            email = "gestor@forgeplan.pt",
            role = "Manager",
            status = "Away",
            projects = "3 Projects"
        ),
        TeamMemberUi(
            initials = "A",
            name = "Ana",
            username = "@ana",
            email = "ana@forgeplan.pt",
            role = "Worker",
            status = "Online",
            projects = "4 Projects"
        ),
        TeamMemberUi(
            initials = "T",
            name = "Tiago",
            username = "@tiago",
            email = "tiago@forgeplan.pt",
            role = "Worker",
            status = "Offline",
            projects = "2 Projects"
        )
    )

    val filteredMembers = members.filter { member ->
        member.name.contains(searchText, ignoreCase = true) ||
                member.username.contains(searchText, ignoreCase = true) ||
                member.email.contains(searchText, ignoreCase = true) ||
                member.role.contains(searchText, ignoreCase = true) ||
                member.status.contains(searchText, ignoreCase = true)
    }

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
                placeholder = "Search your colleagues"
            )

            Spacer(modifier = Modifier.height(24.dp))

            ForgeSectionTitle(text = "Your Team")

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredMembers.isEmpty()) {
                Text(
                    text = "No team members found.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                filteredMembers.forEach { member ->
                    TeamMemberCard(member = member)

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
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
    member: TeamMemberUi
) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            UserAvatarChip(initials = member.initials)

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                ForgeMiniChip(text = "Name")

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForgeMiniChip(text = "Role")

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "• ${member.role}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                ForgeMiniChip(text = "Status")

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "• ${member.status}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(26.dp))

                ForgeMiniChip(text = "Currently in")

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = member.projects,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}