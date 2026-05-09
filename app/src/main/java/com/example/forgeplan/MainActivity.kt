package com.example.forgeplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.forgeplan.core.navigation.AppNavigation
import com.example.forgeplan.ui.theme.ForgePlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ForgePlanTheme {
                AppNavigation()
            }
        }
    }
}