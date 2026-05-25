package com.example.forgeplan.core.network

import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectEvaluation
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.ProjectUserPayload
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskAttachmentPayload
import com.example.forgeplan.core.model.TaskDependency
import com.example.forgeplan.core.model.TaskPayload
import com.example.forgeplan.core.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseService {

    // USERS

    @GET("users")
    fun getUsers(): Call<List<User>>

    // PROJECTS

    @GET("projects")
    fun getProjects(
        @Query("select") select: String = "*"
    ): Call<List<Project>>

    @GET("projects")
    fun getProjectById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Call<List<Project>>

    @Headers("Prefer: return=representation")
    @POST("projects")
    fun createProject(
        @Body project: ProjectPayload
    ): Call<List<Project>>

    @Headers("Prefer: return=representation")
    @PATCH("projects")
    fun updateProject(
        @Query("id") id: String,
        @Body project: ProjectPayload
    ): Call<List<Project>>

    // TASKS

    @GET("tasks")
    fun getTasksByProjectId(
        @Query("project_id") projectId: String,
        @Query("select") select: String = "*"
    ): Call<List<Task>>

    @GET("tasks")
    fun getTaskById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Call<List<Task>>

    @Headers("Prefer: return=representation")
    @POST("tasks")
    fun createTask(
        @Body task: TaskPayload
    ): Call<List<Task>>

    @Headers("Prefer: return=representation")
    @PATCH("tasks")
    fun updateTask(
        @Query("id") id: String,
        @Body task: TaskPayload
    ): Call<List<Task>>

    // TASK ASSIGNMENTS

    @GET("task_assignments")
    fun getTaskAssignmentsByTaskId(
        @Query("task_id") taskId: String,
        @Query("select") select: String = "*"
    ): Call<List<TaskAssignment>>

    @Headers("Prefer: return=representation")
    @POST("task_assignments")
    fun assignUserToTask(
        @Body assignment: TaskAssignment
    ): Call<List<TaskAssignment>>

    // TASK ATTACHMENTS

    @GET("task_attachments")
    fun getTaskAttachmentsByTaskId(
        @Query("task_id") taskId: String,
        @Query("select") select: String = "*"
    ): Call<List<TaskAttachment>>

    @Headers("Prefer: return=representation")
    @POST("task_attachments")
    fun createTaskAttachment(
        @Body attachment: TaskAttachmentPayload
    ): Call<List<TaskAttachment>>

    // PROJECT USERS

    @GET("project_users")
    fun getProjectUsersByProjectId(
        @Query("project_id") projectId: String,
        @Query("select") select: String = "*"
    ): Call<List<ProjectUser>>

    @Headers("Prefer: return=representation")
    @POST("project_users")
    fun assignUserToProject(
        @Body projectUser: ProjectUserPayload
    ): Call<List<ProjectUser>>

    // PROJECT EVALUATIONS

    @GET("project_evaluations")
    fun getProjectEvaluations(
        @Query("project_id") projectId: String,
        @Query("select") select: String = "*"
    ): Call<List<ProjectEvaluation>>

    @Headers("Prefer: return=representation")
    @POST("project_evaluations")
    fun createProjectEvaluation(
        @Body evaluation: ProjectEvaluationPayload
    ): Call<List<ProjectEvaluation>>

    // TASK DEPENDENCIES

    @GET("task_dependencies")
    fun getDependenciesByTaskId(
        @Query("task_id") taskId: String,
        @Query("select") select: String = "*"
    ): Call<List<TaskDependency>>

    @Headers("Prefer: return=representation")
    @POST("task_dependencies")
    fun createDependency(
        @Body dependency: TaskDependency
    ): Call<List<TaskDependency>>
}