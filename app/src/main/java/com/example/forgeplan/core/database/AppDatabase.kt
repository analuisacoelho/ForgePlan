package com.example.forgeplan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.forgeplan.core.database.dao.ProjectDao
import com.example.forgeplan.core.database.dao.TaskDao
import com.example.forgeplan.core.database.dao.UserDao
import com.example.forgeplan.core.database.entity.ProjectEntity
import com.example.forgeplan.core.database.entity.TaskEntity
import com.example.forgeplan.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        TaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
}