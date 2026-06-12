package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.ProjectUserEntity

@Dao
interface ProjectUserDao {

    @Query("SELECT * FROM project_users WHERE project_id = :projectId")
    suspend fun getByProjectId(projectId: Long): List<ProjectUserEntity>

    @Query("SELECT * FROM project_users WHERE user_id = :userId")
    suspend fun getByUserId(userId: Long): List<ProjectUserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projectUsers: List<ProjectUserEntity>)

    @Query("DELETE FROM project_users WHERE project_id = :projectId")
    suspend fun deleteByProjectId(projectId: Long)
}