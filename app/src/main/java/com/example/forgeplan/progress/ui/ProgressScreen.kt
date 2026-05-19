package com.example.forgeplan.progress.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.ui.components.ForgeBigProgressCard
import com.example.forgeplan.core.ui.components.ForgeDropdownCard
import com.example.forgeplan.core.ui.components.ForgeInfoRow
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryLargeButton
import com.example.forgeplan.core.ui.components.ForgeSectionTitle

@Composable
fun ProgressScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {}
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
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            ForgeSectionTitle(text = "Progress")

            Spacer(modifier = Modifier.height(18.dp))

            ForgeDropdownCard(
                text = "Select your project",
                icon = Icons.Outlined.AccountCircle
            )

            Spacer(modifier = Modifier.height(10.dp))

            ForgeDropdownCard(
                text = "Select your task",
                icon = Icons.Outlined.CheckCircle
            )

            Spacer(modifier = Modifier.height(24.dp))

            ForgeBigProgressCard(
                progress = 39
            )

            Spacer(modifier = Modifier.height(16.dp))

            ForgeInfoRow(
                title = "Time spent",
                value = "3 hours",
                icon = Icons.Outlined.CheckCircle
            )

            Spacer(modifier = Modifier.height(10.dp))

            ForgeInfoRow(
                title = "Location",
                value = "Workshop A",
                icon = Icons.Outlined.AccountCircle
            )

            Spacer(modifier = Modifier.height(10.dp))

            AttachmentCard()

            Spacer(modifier = Modifier.height(10.dp))

            NotesCard()

            Spacer(modifier = Modifier.height(12.dp))

            ForgePrimaryLargeButton(
                text = "Save progress",
                onClick = {}
            )
        }

        ForgePlanBottomBar(
            selectedItem = "Progress",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun AttachmentCard() {
    ForgeOutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "Photo attachment",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UploadBox(
                    modifier = Modifier.weight(1f)
                )

                PreviewBox(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun UploadBox(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun PreviewBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun NotesCard() {
    ForgeOutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .height(132.dp)
                .padding(14.dp)
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Here you can write about your progress and any obstacles you may have encountered.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}