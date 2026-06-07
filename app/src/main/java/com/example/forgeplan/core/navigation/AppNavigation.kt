package com.example.forgeplan.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.forgeplan.admin.ui.AdminDashboardScreen
import com.example.forgeplan.auth.ui.LoginScreen
import com.example.forgeplan.profile.ProfileScreen
import com.example.forgeplan.progress.ui.ProgressScreen
import com.example.forgeplan.projects.ui.CreateProjectScreen
import com.example.forgeplan.projects.ui.EditProjectScreen
import com.example.forgeplan.projects.ui.ManagerDashboardScreen
import com.example.forgeplan.projects.ui.ProjectDetailScreen
import com.example.forgeplan.projects.ui.ProjectReviewScreen
import com.example.forgeplan.reports.ui.ReportsScreen
import com.example.forgeplan.social.ui.UserPublicScreen
import com.example.forgeplan.tasks.ui.CreateTaskScreen
import com.example.forgeplan.tasks.ui.EditTaskScreen
import com.example.forgeplan.tasks.ui.ProjectTasksScreen
import com.example.forgeplan.tasks.ui.TaskDetailScreen
import com.example.forgeplan.tasks.ui.UserDashboardScreen
import com.example.forgeplan.team.ui.TeamScreen
import com.example.forgeplan.timeline.ui.TimelineScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    when (role) {
                        "ADMIN" -> navController.navigate("admin") {
                            popUpTo("login") { inclusive = true }
                        }

                        "MANAGER" -> navController.navigate("manager") {
                            popUpTo("login") { inclusive = true }
                        }

                        else -> navController.navigate("user") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("admin") {
            AdminDashboardScreen()
        }

        composable("manager") {
            ManagerDashboardScreen(
                onProjectClick = { navController.navigate("projectDetail/$it") },
                onCreateProjectClick = { navController.navigate("createProject") },
                onEditTaskClick = { navController.navigate("editTask/$it") },
                onTimelineClick = { navController.navigate("timeline") },
                onProgressClick = { navController.navigate("reports") },
                onTeamClick = { navController.navigate("team") }
            )
        }

        composable("createProject") {
            CreateProjectScreen(
                onProjectCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = "editProject/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            EditProjectScreen(
                projectId = back.arguments?.getLong("projectId") ?: 0L,
                onProjectUpdated = { navController.popBackStack() }
            )
        }

        composable(
            route = "projectDetail/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            val projectId = back.arguments?.getLong("projectId") ?: 0L

            ProjectDetailScreen(
                projectId = projectId,
                onCreateTaskClick = { navController.navigate("createTask/$projectId") },
                onEditProjectClick = { navController.navigate("editProject/$projectId") },

                // IMPORTANTe:
                // Agora o botão Details abre o detalhe da task,
                // não o ecrã de editar.
                onTaskClick = { taskId -> navController.navigate("taskDetail/$taskId") },

                onReviewProjectClick = { navController.navigate("projectReview/$projectId") },
                onProjectsClick = {
                    navController.navigate("manager") {
                        popUpTo("manager") { inclusive = true }
                    }
                },
                onTimelineClick = { navController.navigate("timeline") },
                onProgressClick = { navController.navigate("reports") },
                onTeamClick = { navController.navigate("team") }
            )
        }

        composable(
            route = "projectReview/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            ProjectReviewScreen(
                projectId = back.arguments?.getLong("projectId") ?: 0L,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "taskDetail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { back ->
            val taskId = back.arguments?.getLong("taskId") ?: 0L

            TaskDetailScreen(
                taskId = taskId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { navController.navigate("editTask/$it") }
            )
        }

        composable("timeline") {
            TimelineScreen(
                onProjectsClick = { navController.navigate("manager") },
                onProgressClick = { navController.navigate("reports") },
                onTeamClick = { navController.navigate("team") }
            )
        }

        composable("reports") {
            ReportsScreen(
                onProjectsClick = { navController.navigate("manager") },
                onTimelineClick = { navController.navigate("timeline") },
                onTeamClick = { navController.navigate("team") }
            )
        }

        composable("team") {
            TeamScreen(
                onProjectsClick = { navController.navigate("manager") },
                onTimelineClick = { navController.navigate("timeline") },
                onProgressClick = { navController.navigate("reports") }
            )
        }

        composable("createTask") {
            CreateTaskScreen(
                projectId = null,
                onTaskCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = "createTask/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            CreateTaskScreen(
                projectId = back.arguments?.getLong("projectId"),
                onTaskCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = "editTask/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { back ->
            EditTaskScreen(
                taskId = back.arguments?.getLong("taskId") ?: 0L,
                onTaskUpdated = { navController.popBackStack() }
            )
        }

        composable(
            route = "projectTasks/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            ProjectTasksScreen(
                projectId = back.arguments?.getLong("projectId") ?: 0L,
                onBack = { navController.popBackStack() },
                onTaskClick = { navController.navigate("userProgress/$it") },
                onTimelineClick = { navController.navigate("userTimeline") },
                onProgressClick = { navController.navigate("userProgress") },
                onTeamClick = { navController.navigate("userTeam") },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        composable("user") {
            UserDashboardScreen(
                onProjectClick = { navController.navigate("projectTasks/$it") },
                onTimelineClick = { navController.navigate("userTimeline") },
                onProgressClick = { navController.navigate("userProgress") },
                onTeamClick = { navController.navigate("userTeam") },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        composable("userTimeline") {
            TimelineScreen(
                onProjectsClick = { navController.navigate("user") },
                onProgressClick = { navController.navigate("userProgress") },
                onTeamClick = { navController.navigate("userTeam") }
            )
        }

        composable(
            route = "userProgress/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { back ->
            ProgressScreen(
                taskId = back.arguments?.getLong("taskId") ?: 0L,
                onProjectsClick = { navController.navigate("user") },
                onTimelineClick = { navController.navigate("userTimeline") },
                onTeamClick = { navController.navigate("userTeam") }
            )
        }

        composable("userProgress") {
            ProgressScreen(
                taskId = 0L,
                onProjectsClick = { navController.navigate("user") },
                onTimelineClick = { navController.navigate("userTimeline") },
                onTeamClick = { navController.navigate("userTeam") }
            )
        }

        composable("userPublic") {
            UserPublicScreen(
                onProjectsClick = { navController.navigate("user") },
                onTimelineClick = { navController.navigate("userTimeline") },
                onTeamClick = { navController.navigate("userTeam") }
            )
        }

        composable("userTeam") {
            TeamScreen(
                onProjectsClick = { navController.navigate("user") },
                onTimelineClick = { navController.navigate("userTimeline") },
                onProgressClick = { navController.navigate("userProgress") }
            )
        }

        composable("profile") {
            ProfileScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}