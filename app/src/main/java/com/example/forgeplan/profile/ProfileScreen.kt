package com.example.forgeplan.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeAvatar
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onSecurity: () -> Unit = {},
    onHelpCenter: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        ForgePlanTopBar(title = "ForgePlan")

        Spacer(modifier = Modifier.height(16.dp))

        // Cartão utilizador
        ForgeOutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                ForgeAvatar(
                    initials = SessionManager.userInitials,
                    size = 70
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = SessionManager.currentUser?.name ?: "User",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileSection(
            title = "Account",
            items = listOf(
                "Edit Profile" to onEditProfile,
                "Change Password" to onChangePassword,
                "Security" to onSecurity
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileSection(
            title = "Support",
            items = listOf(
                "Help Center" to onHelpCenter,
                "About ForgePlan" to onAbout
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        ForgeSecondaryButton(
            text = "Logout",
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogout
        )
    }
}

@Composable
fun ProfileSection(
    title: String,
    items: List<Pair<String, () -> Unit>>
) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { (label, action) ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { action() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = label,
                        modifier = Modifier.weight(1f)
                    )

                    Text(">")
                }
            }
        }
    }
}