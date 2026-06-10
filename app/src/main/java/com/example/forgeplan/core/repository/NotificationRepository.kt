package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Notification
import com.example.forgeplan.core.model.NotificationPayload
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository {

    private val api = SupabaseApi.service

    /** Busca todas as notificações do utilizador (mais recentes primeiro). */
    suspend fun getNotificationsForUser(userId: Long): List<Notification> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getNotificationsByUserId(
                    userId = "eq.$userId",
                    order = "created_at.desc"
                ).execute()
                response.body() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Conta notificações não lidas do utilizador (para o badge). */
    suspend fun getUnreadCount(userId: Long): Int =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getNotificationsByUserId(
                    userId = "eq.$userId",
                    isRead = "eq.false"
                ).execute()
                response.body()?.size ?: 0
            } catch (e: Exception) {
                0
            }
        }

    /** Marca uma notificação como lida. */
    suspend fun markAsRead(notificationId: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = api.markNotificationRead(
                    id = "eq.$notificationId",
                    body = mapOf("is_read" to true)
                ).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }

    /** Marca TODAS as notificações do utilizador como lidas. */
    suspend fun markAllAsRead(userId: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = api.markAllNotificationsRead(
                    userId = "eq.$userId",
                    body = mapOf("is_read" to true)
                ).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Cria uma notificação.
     * Chamado após inserir comentário, alterar estado de tarefa, etc.
     */
    suspend fun createNotification(payload: NotificationPayload): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createNotification(payload).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
}