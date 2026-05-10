package com.example.forgeplan.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.forgeplan.R

@Composable
fun WelcomeScreen(
    onRoleSelected: (String) -> Unit
) {
    val darkBlue = Color(0xFF17184A)
    val beige = Color(0xFFEED7AD)

    var selectedLanguage by remember { mutableStateOf("EN") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageButton(
                text = "us EN",
                selected = selectedLanguage == "EN",
                onClick = { selectedLanguage = "EN" }
            )

            LanguageButton(
                text = "pt PT",
                selected = selectedLanguage == "PT",
                onClick = { selectedLanguage = "PT" }
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.forgeplan_logo),
                contentDescription = "ForgePlan logo",
                modifier = Modifier.size(150.dp)
            )

            Text(
                text = "ForgePlan",
                color = beige,
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = if (selectedLanguage == "PT") "Entrar como" else "Login as",
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RoleButton(
                    text = if (selectedLanguage == "PT") "Utilizador" else "Worker",
                    selected = true,
                    modifier = Modifier.weight(1f)
                ) {
                    onRoleSelected("USER")
                }

                RoleButton(
                    text = if (selectedLanguage == "PT") "Gestor" else "Manager",
                    selected = false,
                    modifier = Modifier.weight(1f)
                ) {
                    onRoleSelected("MANAGER")
                }

                RoleButton(
                    text = "Admin",
                    selected = false,
                    modifier = Modifier.weight(1f)
                ) {
                    onRoleSelected("ADMIN")
                }
            }
        }
    }
}

@Composable
fun LanguageButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFEED7AD) else Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun RoleButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFEED7AD) else Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text)
    }
}