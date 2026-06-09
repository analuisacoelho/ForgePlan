package com.example.forgeplan.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar


/**
 * Scaffold partilhado por todos os ecrãs do Admin.
 */

@Composable
fun AdminScaffold(
    selectedItem: String = "Projects",
    onProjectsClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = SessionManager.userInitials
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }

        ForgePlanBottomBar(
            selectedItem = selectedItem,
            onProjectsClick = onProjectsClick,
            onUsersClick = onUsersClick,
            onActivityClick = onActivityClick,
            onProfileClick = onProfileClick
        )
    }
}