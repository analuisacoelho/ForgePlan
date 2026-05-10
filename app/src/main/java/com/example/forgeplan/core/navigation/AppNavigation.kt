package com.example.forgeplan.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.forgeplan.admin.ui.AdminDashboardScreen
import com.example.forgeplan.auth.ui.LoginScreen
import com.example.forgeplan.auth.ui.WelcomeScreen
import com.example.forgeplan.projects.ui.ManagerDashboardScreen
import com.example.forgeplan.tasks.ui.UserDashboardScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                onRoleSelected = { role ->
                    navController.navigate("login/$role")
                }
            )
        }

        composable(
            route = "login/{role}",
            arguments = listOf(
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "USER"

            LoginScreen(
                selectedRole = role,
                onLoginSuccess = {
                    when (role) {
                        "ADMIN" -> navController.navigate("admin")
                        "MANAGER" -> navController.navigate("manager")
                        else -> navController.navigate("user")
                    }
                }
            )
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