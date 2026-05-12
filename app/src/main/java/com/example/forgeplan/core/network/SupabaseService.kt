package com.example.forgeplan.core.network

import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseService {

    @GET("users")
    fun getUsers(): Call<List<User>>

    @GET("projects")
    fun getProjects(
        @Query("select") select: String = "*"
    ): Call<List<Project>>

    @GET("projects")
    fun getProjectById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
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

    @POST("tasks")
    fun createTask(
        @Body task: Task
    ): Call<Task>

    @PATCH("tasks")
    fun updateTask(
        @Query("id") id: String,
        @Body task: Task
    ): Call<Task>

    @Headers("Prefer: return=representation")
    @POST("task_assignments")
    fun assignUserToTask(
        @Body assignment: TaskAssignment
    ): Call<List<TaskAssignment>>
}