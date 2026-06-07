package com.example.forgeplan.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSideMenuScaffold
import com.example.forgeplan.core.ui.components.SideMenuItem
import kotlinx.coroutines.launch

/**
 * Scaffold partilhado por todos os ecrãs do Admin.
 * Centraliza a topbar, bottombar e side menu num único componente.
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Sidemenu
    ForgeSideMenuScaffold(
        selectedItem = "",
        drawerState = drawerState,
        onLogout = {
            SessionManager.clear()
            onLogout()
        },
        items = listOf(
            SideMenuItem(appText(en = "Profile", pt = "Perfil"), "◎", "Profile", onProfileClick)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Clicar nas iniciais abre o side menu
            ForgePlanTopBar(
                title = "ForgePlan",
                initials = SessionManager.userInitials,
                onAvatarClick = { scope.launch { drawerState.open() } }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }

            // Bottombar
            ForgePlanBottomBar(
                selectedItem = selectedItem,
                onProjectsClick = onProjectsClick,
                onUsersClick = onUsersClick,
                onActivityClick = onActivityClick,
                onProfileClick = onProfileClick
            )
        }
    }
}