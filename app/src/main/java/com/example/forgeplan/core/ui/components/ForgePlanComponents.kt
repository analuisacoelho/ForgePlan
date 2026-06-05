package com.example.forgeplan.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.launch

@Composable
fun ForgePlanTopBar(
    title: String = "ForgePlan",
    initials: String = "UN",
    // Callback para abrir o side menu ao clicar nas iniciais
    onAvatarClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clicar nas iniciais abre o side menu
            ForgeAvatar(
                initials = initials,
                size = 34,
                modifier = Modifier.clickable { onAvatarClick() }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = appText(en = "Notifications", pt = "Notificações"),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
fun ForgePlanBottomBar(
    selectedItem: String = "Tasks",
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForgeBottomBarItem(
                label = appText(en = "Tasks", pt = "Tarefas"),
                icon = "☑",
                selected = selectedItem == "Projects" || selectedItem == "Tasks",
                onClick = onProjectsClick
            )

            ForgeBottomBarItem(
                label = appText(en = "Timeline", pt = "Cronologia"),
                icon = "◷",
                selected = selectedItem == "Timeline",
                onClick = onTimelineClick
            )

            ForgeBottomBarItem(
                label = appText(en = "Reports", pt = "Relatórios"),
                icon = "▤",
                selected = selectedItem == "Reports",
                onClick = onProgressClick
            )

            ForgeBottomBarItem(
                label = appText(en = "Team", pt = "Equipa"),
                icon = "♧",
                selected = selectedItem == "Team",
                onClick = onTeamClick
            )

            ForgeBottomBarItem(
                label = appText(en = "Profile", pt = "Perfil"),
                icon = "◎",
                selected = selectedItem == "Profile",
                onClick = onProfileClick
            )
        }
    }
}

@Composable
fun ForgeBottomBarItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(70.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondary
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1
        )
    }
}

@Composable
fun ForgeAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Int = 30,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun UserAvatarChip(
    initials: String,
    modifier: Modifier = Modifier
) {
    ForgeAvatar(
        initials = initials,
        modifier = modifier
    )
}

@Composable
fun StatusChip(
    text: String
) {
    ForgeMiniChip(text = text)
}

@Composable
fun ForgeMiniChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun ForgeOutlinedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun ForgePrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(42.dp),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun ForgePrimaryLargeButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ForgeSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier.height(42.dp),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ForgeSectionTitle(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ForgeSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ForgeDropdownCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    ForgeOutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ForgeInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    ForgeOutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            ForgeMiniChip(text = value)
        }
    }
}

@Composable
fun ForgeBigProgressCard(
    progress: Int,
    modifier: Modifier = Modifier
) {
    val progressFraction = progress.coerceIn(0, 100) / 100f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◷",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = appText(en = "Progress", pt = "Progresso"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )

                ForgeMiniChip(
                    text = "$progress%",
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "0%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

data class SideMenuItem(
    val label: String,
    val icon: String,
    val key: String,
    val onClick: () -> Unit
)

@Composable
fun ForgeSideMenuScaffold(
    selectedItem: String,
    items: List<SideMenuItem>,
    onLogout: () -> Unit,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Header com iniciais e nome do utilizador
                    Row(
                        modifier = Modifier.padding(vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ForgeAvatar(
                            initials = SessionManager.userInitials,
                            size = 48
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = SessionManager.currentUser?.name ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = SessionManager.currentUser?.role
                                    ?.lowercase()
                                    ?.replaceFirstChar { it.uppercase() } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Itens de navegação
                    items.forEach { item ->
                        SideMenuItemRow(
                            item = item,
                            selected = selectedItem == item.key,
                            onClick = {
                                scope.launch { drawerState.close() }
                                item.onClick()
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout no fundo do menu
                    SideMenuItemRow(
                        item = SideMenuItem(
                            label = appText(en = "Logout", pt = "Sair"),
                            icon = "⏻",
                            key = "Logout",
                            onClick = onLogout
                        ),
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        }
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun SideMenuItemRow(
    item: SideMenuItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(248.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondary
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = item.icon,
            style = MaterialTheme.typography.titleLarge,
            color = if (selected)
                MaterialTheme.colorScheme.onSecondary
            else
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected)
                MaterialTheme.colorScheme.onSecondary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
