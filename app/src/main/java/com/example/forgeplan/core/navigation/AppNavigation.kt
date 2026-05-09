package com.example.forgeplan.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.forgeplan.admin.ui.AdminDashboardScreen
import com.example.forgeplan.auth.ui.LoginScreen
import com.example.forgeplan.projects.ui.ManagerDashboardScreen
import com.example.forgeplan.tasks.ui.UserDashboardScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen()
        }

        composable("admin") {
            AdminDashboardScreen()
        }

        composable("manager") {
            ManagerDashboardScreen()
        }

        composable("user") {
            UserDashboardScreen()
        }
    }
}