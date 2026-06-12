package com.example.forgeplan

import android.app.Application
import com.example.forgeplan.core.database.AppDatabase
import com.example.forgeplan.core.database.DatabaseProvider

class ForgePlanApplication : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = DatabaseProvider.getDatabase(this)
    }
}