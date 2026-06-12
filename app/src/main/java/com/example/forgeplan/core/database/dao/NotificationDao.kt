package com.example.forgeplan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.forgeplan.core.database.entity.NotificationEntity

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getByUserId(userId: Long): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = 0")
    suspend fun getUnreadByUserId(userId: Long): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    suspend fun markAllRead(userId: Long)
}