package com.example.forgeplan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.forgeplan.core.database.dao.CommentDao
import com.example.forgeplan.core.database.dao.NotificationDao
import com.example.forgeplan.core.database.dao.ProjectDao
import com.example.forgeplan.core.database.dao.ProjectUserDao
import com.example.forgeplan.core.database.dao.TaskAssignmentDao
import com.example.forgeplan.core.database.dao.TaskAttachmentDao
import com.example.forgeplan.core.database.dao.TaskDao
import com.example.forgeplan.core.database.dao.TaskLogDao
import com.example.forgeplan.core.database.dao.TaskPhotoDao
import com.example.forgeplan.core.database.dao.UserDao
import com.example.forgeplan.core.database.entity.CommentEntity
import com.example.forgeplan.core.database.entity.NotificationEntity
import com.example.forgeplan.core.database.entity.ProjectEntity
import com.example.forgeplan.core.database.entity.ProjectUserEntity
import com.example.forgeplan.core.database.entity.TaskAssignmentEntity
import com.example.forgeplan.core.database.entity.TaskAttachmentEntity
import com.example.forgeplan.core.database.entity.TaskEntity
import com.example.forgeplan.core.database.entity.TaskLogEntity
import com.example.forgeplan.core.database.entity.TaskPhotoEntity
import com.example.forgeplan.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        TaskLogEntity::class,
        TaskPhotoEntity::class,
        TaskAttachmentEntity::class,
        CommentEntity::class,
        ProjectUserEntity::class,
        TaskAssignmentEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun taskPhotoDao(): TaskPhotoDao
    abstract fun taskAttachmentDao(): TaskAttachmentDao
    abstract fun commentDao(): CommentDao
    abstract fun projectUserDao(): ProjectUserDao
    abstract fun taskAssignmentDao(): TaskAssignmentDao
    abstract fun notificationDao(): NotificationDao
}