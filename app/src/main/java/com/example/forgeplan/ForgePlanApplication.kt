package com.example.forgeplan

import android.app.Application
import com.example.forgeplan.core.database.AppDatabase
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.sync.ConnectivityObserver
import com.example.forgeplan.core.sync.SyncManager

class ForgePlanApplication : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = DatabaseProvider.getDatabase(this)

        // Tenta sincronizar pendentes logo ao abrir a app (se já houver rede)
        SyncManager.syncIfOnline(this)

        // Regista o listener que dispara a sincronização sempre que a rede voltar
        ConnectivityObserver.register(this)
    }

    companion object {
        lateinit var instance: ForgePlanApplication
            private set
    }
}