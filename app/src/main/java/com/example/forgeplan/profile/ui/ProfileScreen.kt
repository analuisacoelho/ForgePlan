package com.example.forgeplan.profile.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.forgeplan.admin.ui.AdminScaffold
import com.example.forgeplan.core.language.AppLanguage
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeAvatar
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {},
    onProjectsClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProgressClick: () -> Unit = {}
) {
    val role = SessionManager.userRole

    when (role) {
        "ADMIN" -> AdminScaffold(
            selectedItem = "Profile",
            onProjectsClick = onProjectsClick,
            onUsersClick = onUsersClick,
            onActivityClick = onActivityClick,
            onProfileClick = {},
            onLogout = onLogout
        ) {
            ProfileContent(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onEditProfile = onEditProfile,
                onChangePassword = onChangePassword,
                onLogout = onLogout
            )
        }

        "MANAGER" -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ForgePlanTopBar(title = "ForgePlan", initials = SessionManager.userInitials)
            Box(modifier = Modifier.weight(1f)) {
                ProfileContent(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onLogout = onLogout
                )
            }
            ForgePlanBottomBar(
                selectedItem = "Profile",
                onProjectsClick = onProjectsClick,
                onTimelineClick = onTimelineClick,
                onProgressClick = onReportsClick,
                onTeamClick = onTeamClick,
                onProfileClick = {}
            )
        }

        else -> Column( // USER
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ForgePlanTopBar(title = "ForgePlan", initials = SessionManager.userInitials)
            Box(modifier = Modifier.weight(1f)) {
                ProfileContent(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onLogout = onLogout
                )
            }
            ForgePlanBottomBar(
                selectedItem = "Profile",
                onProjectsClick = onProjectsClick,
                onTimelineClick = onTimelineClick,
                onTeamClick = onTeamClick,
                onProfileClick = {}
            )
        }
    }
}

@Composable
private fun ProfileContent(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val photoUrl = SessionManager.currentUser?.photo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = if (isLandscape) 32.dp else 18.dp,
                vertical = 16.dp
            )
    ) {
        // ── Avatar card ──────────────────────────────────────────────
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ForgeAvatar(initials = SessionManager.userInitials, size = 72)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = SessionManager.currentUser?.name
                        ?: appText(en = "User", pt = "Utilizador"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SessionManager.currentUser?.email?.let { email ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Account section ──────────────────────────────────────────
        ProfileSection(
            title = appText(en = "Account", pt = "Conta"),
            items = listOf(
                appText(en = "Edit Profile", pt = "Editar Perfil") to onEditProfile,
                appText(en = "Change Password", pt = "Alterar Password") to onChangePassword
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Preferences section (Language + Theme) ───────────────────
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = appText(en = "Preferences", pt = "Preferências"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Language toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appText(en = "Language", pt = "Idioma"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SegmentedToggle(
                        options = listOf("EN", "PT"),
                        selected = AppLanguage.current,
                        onSelect = { AppLanguage.set(it) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Theme toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appText(en = "Theme", pt = "Tema"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SegmentedToggle(
                        options = listOf(
                            appText(en = "Light", pt = "Claro"),
                            appText(en = "Dark", pt = "Escuro")
                        ),
                        selected = if (isDarkTheme)
                            appText(en = "Dark", pt = "Escuro")
                        else
                            appText(en = "Light", pt = "Claro"),
                        onSelect = { choice ->
                            val wantsDark = choice == appText(en = "Dark", pt = "Escuro")
                            if (wantsDark != isDarkTheme) onToggleTheme()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ForgeSecondaryButton(
            text = appText(en = "Logout", pt = "Terminar sessão"),
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogout
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Small segmented control ──────────────────────────────────────────────────
@Composable
private fun SegmentedToggle(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val activeBg = if (isDark) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary
    val activeText = if (isDark) MaterialTheme.colorScheme.onSecondary
    else MaterialTheme.colorScheme.onPrimary
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.Center
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .background(if (isSelected) activeBg else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) activeText
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Reusable section ─────────────────────────────────────────────────────────
@Composable
fun ProfileSection(
    title: String,
    items: List<Pair<String, () -> Unit>>
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            items.forEachIndexed { index, (label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { action() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = ">", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                if (index < items.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }
}