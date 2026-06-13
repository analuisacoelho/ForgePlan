package com.example.forgeplan.core.repository

import com.example.forgeplan.core.language.AppLanguage
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService
import com.example.forgeplan.core.session.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivityLogRepository {

    fun logActivity(
        action: String,
        entityType: String,
        entityId: Long,
        detailsEn: String,
        detailsPt: String
    ) {
        val details = if (AppLanguage.isPortuguese()) detailsPt else detailsEn
        val payload = SupabaseService.ActivityLogPayload(
            user_id = SessionManager.userId,
            action = action,
            entity_type = entityType,
            entity_id = entityId,
            details = details
        )
        SupabaseApi.service.createActivityLog(payload)
            .enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    // Fire-and-forget: log successful if needed for debug
                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    // Fire-and-forget: ignore log failure
                }
            })
    }
}