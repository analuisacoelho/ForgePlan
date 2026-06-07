package com.example.forgeplan.core.network

import com.example.forgeplan.core.model.Comment
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
import com.example.forgeplan.core.model.TaskGroup
import com.example.forgeplan.core.model.TaskGroupPayload
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.TaskPayload
import com.example.forgeplan.core.model.TaskPhoto
import com.example.forgeplan.core.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseService {

    @GET("users")
    fun getUsers(): Call<List<User>>

    @GET("users")
    fun getUserByEmail(
        @Query("email") email: String,
        @Query("select") select: String = "*"
    ): Call<List<User>>

    @Headers("Prefer: return=representation")
    @POST("users")
    fun createUser(@Body user: User): Call<List<User>>

    @Headers("Prefer: return=representation")
    @PATCH("users")
    fun updateUser(
        @Query("id") id: String,
        @Body user: User
    ): Call<List<User>>

    @GET("projects")
    fun getProjects(@Query("select") select: String = "*"): Call<List<Project>>

    @GET("projects")
    fun getProjectById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Call<List<Project>>

    @Headers("Prefer: return=representation")
    @POST("projects")
    fun createProject(@Body project: ProjectPayload): Call<List<Project>>

    @Headers("Prefer: return=representation")
    @PATCH("projects")
    fun updateProject(
        @Query("id") id: String,
        @Body project: ProjectPayload
    ): Call<List<Project>>

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
    fun createTask(@Body task: TaskPayload): Call<List<Task>>

    @Headers("Prefer: return=representation")
    @PATCH("tasks")
    fun updateTask(
        @Query("id") id: String,
        @Body task: TaskPayload
    ): Call<List<Task>>

    data class TaskLogPayload(
        val task_id: Long,
        val user_id: Long,
        val log_date: String,
        val location: String,
        val completion_rate: Int,
        val minutes_spent: Int,
        val notes: String,
        val is_synced: Boolean = true
    )

    @Headers("Prefer: return=representation")
    @POST("task_logs")
    suspend fun insertTaskLog(@Body log: TaskLogPayload): List<TaskLog>

    @GET("task_logs")
    fun getTaskLogsByTaskId(
        @Query("task_id") taskId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Call<List<TaskLog>>

    data class TaskPhotoPayload(
        val task_log_id: Long,
        val photo_url: String,
        val captured_at: String,
        val is_synced: Boolean = true
    )

    @Headers("Prefer: return=representation")
    @POST("task_photos")
    suspend fun insertTaskPhoto(@Body photo: TaskPhotoPayload): List<TaskPhoto>

    @GET("task_photos")
    fun getTaskPhotosByLogId(
        @Query("task_log_id") taskLogId: String,
        @Query("select") select: String = "*"
    ): Call<List<TaskPhoto>>

    data class CommentRequest(
        val task_id: Long,
        val user_id: Long,
        val content: String
    )

    @Headers("Prefer: return=representation")
    @POST("comments")
    suspend fun insertComment(@Body comment: CommentRequest): List<Comment>

    @GET("comments")
    fun getCommentsByTaskId(
        @Query("task_id") taskId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.asc"
    ): Call<List<Comment>>

    @GET("task_assignments")
    fun getTaskAssignmentsByTaskId(
        @Query("task_id") taskId: String,
        @Query("select") select: String = "*"
    ): Call<List<TaskAssignment>>

    @GET("task_assignments")
    fun getTaskAssignmentsByUserId(
        @Query("user_id") userId: String,
        @Query("select") select: String = "*"
    ): Call<List<TaskAssignment>>

    @Headers("Prefer: return=representation")
    @POST("task_assignments")
    fun assignUserToTask(
        @Body assignment: TaskAssignment
    ): Call<List<TaskAssignment>>

    @DELETE("task_assignments")
    fun removeUserFromTask(
        @Query("task_id") taskIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Call<Void>

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

    @GET("project_users")
    fun getProjectUsersByProjectId(
        @Query("project_id") projectId: String,
        @Query("select") select: String = "*"
    ): Call<List<ProjectUser>>

    @GET("project_users")
    fun getProjectUsersByUserId(
        @Query("user_id") userId: String,
        @Query("select") select: String = "*"
    ): Call<List<ProjectUser>>

    @Headers("Prefer: return=representation")
    @POST("project_users")
    fun assignUserToProject(
        @Body projectUser: ProjectUserPayload
    ): Call<List<ProjectUser>>

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

    @GET("task_groups")
    fun getTaskGroupsByProjectId(
        @Query("project_id") projectId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.asc"
    ): Call<List<TaskGroup>>

    @Headers("Prefer: return=representation")
    @POST("task_groups")
    fun createTaskGroup(
        @Body group: TaskGroupPayload
    ): Call<List<TaskGroup>>
}