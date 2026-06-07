package com.example.forgeplan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(

    primary = ForgePrimary,
    onPrimary = ForgeWhite,

    secondary = ForgeGold,
    onSecondary = ForgeBlack,

    background = ForgeBackground,
    onBackground = ForgeBlack,

    surface = ForgeSurface,
    onSurface = ForgeBlack,

    secondaryContainer = ForgeSurfaceDark,
    onSecondaryContainer = ForgeBlack,

    tertiary = ForgePurple,
    onTertiary = ForgeWhite,

    error = ForgeError
)

private val DarkColorScheme = darkColorScheme(
    primary = ForgeDarkSurface,
    onPrimary = ForgeWhite,

    secondary = ForgeGold,
    onSecondary = ForgeBlack,

    background = ForgeDark,
    onBackground = ForgeWhite,

    surface = ForgeDarkSurfaceElevated,
    onSurface = ForgeWhite,

    secondaryContainer = ForgePurple,
    onSecondaryContainer = ForgeWhite,

    tertiary = ForgeSurfaceDark,
    onTertiary = ForgeBlack,

    error = ForgeError
)

@Composable
fun ForgePlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) DarkColorScheme
        else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}