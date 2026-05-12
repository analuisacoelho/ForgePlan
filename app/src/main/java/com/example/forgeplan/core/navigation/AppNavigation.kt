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
import com.example.forgeplan.projects.ui.ProjectDetailScreen
import com.example.forgeplan.tasks.ui.CreateTaskScreen
import com.example.forgeplan.tasks.ui.EditTaskScreen
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
            ManagerDashboardScreen(
                onProjectClick = { projectId ->
                    navController.navigate("projectDetail/$projectId")
                }
            )
        }

        composable(
            route = "projectDetail/{projectId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L

            ProjectDetailScreen(
                projectId = projectId,
                onCreateTaskClick = {
                    navController.navigate("createTask/$projectId")
                },
                onTaskClick = { taskId ->
                    navController.navigate("editTask/$taskId")
                }
            )
        }

        composable(
            route = "createTask/{projectId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L

            CreateTaskScreen(
                projectId = projectId,
                onTaskCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "editTask/{taskId}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L

            EditTaskScreen(
                taskId = taskId,
                onTaskUpdated = {
                    navController.popBackStack()
                }
            )
        }

        composable("user") {
            UserDashboardScreen()
        }
    }
}