package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.CommentEntity

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE task_id = :taskId ORDER BY created_at ASC")
    suspend fun getCommentsByTaskId(taskId: Long): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE is_synced = 0")
    suspend fun getUnsynced(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("UPDATE comments SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}