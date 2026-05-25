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
import com.example.forgeplan.progress.ui.ProgressScreen
import com.example.forgeplan.projects.ui.CreateProjectScreen
import com.example.forgeplan.projects.ui.EditProjectScreen
import com.example.forgeplan.projects.ui.ManagerDashboardScreen
import com.example.forgeplan.projects.ui.ProjectDetailScreen
import com.example.forgeplan.projects.ui.ProjectReviewScreen
import com.example.forgeplan.tasks.ui.CreateTaskScreen
import com.example.forgeplan.tasks.ui.EditTaskScreen
import com.example.forgeplan.tasks.ui.UserDashboardScreen
import com.example.forgeplan.team.ui.TeamScreen
import com.example.forgeplan.timeline.ui.TimelineScreen

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
                    navController.navigate("projectReview/$projectId")
                },
                onCreateProjectClick = {
                    navController.navigate("createTask")
                },
                onEditTaskClick = { taskId ->
                    navController.navigate("editTask/$taskId")
                },
                onTimelineClick = {
                    navController.navigate("timeline")
                },
                onProgressClick = {
                    navController.navigate("progress")
                },
                onTeamClick = {
                    navController.navigate("team")
                }
            )
        }

        composable("createProject") {
            CreateProjectScreen(
                onProjectCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "editProject/{projectId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L

            EditProjectScreen(
                projectId = projectId,
                onProjectUpdated = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "projectReview/{projectId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L

            ProjectReviewScreen(
                projectId = projectId,
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("timeline") {
            TimelineScreen(
                onProjectsClick = {
                    navController.navigate("manager")
                },
                onProgressClick = {
                    navController.navigate("progress")
                },
                onTeamClick = {
                    navController.navigate("team")
                }
            )
        }

        composable("progress") {
            ProgressScreen(
                onProjectsClick = {
                    navController.navigate("manager")
                },
                onTimelineClick = {
                    navController.navigate("timeline")
                },
                onTeamClick = {
                    navController.navigate("team")
                }
            )
        }

        composable("team") {
            TeamScreen(
                onProjectsClick = {
                    navController.navigate("manager")
                },
                onTimelineClick = {
                    navController.navigate("timeline")
                },
                onProgressClick = {
                    navController.navigate("progress")
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
                onEditProjectClick = {
                    navController.navigate("editProject/$projectId")
                },
                onTaskClick = { taskId ->
                    navController.navigate("editTask/$taskId")
                },
                onProjectsClick = {
                    navController.navigate("manager")
                },
                onTimelineClick = {
                    navController.navigate("timeline")
                },
                onProgressClick = {
                    navController.navigate("progress")
                },
                onTeamClick = {
                    navController.navigate("team")
                }
            )
        }

        composable("createTask") {
            CreateTaskScreen(
                projectId = null,
                onTaskCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "createTask/{projectId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId")

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