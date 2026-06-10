package com.example.forgeplan.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Notification
import com.example.forgeplan.core.repository.NotificationRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repo = NotificationRepository()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ── Filtros ──────────────────────────────────────────────────────────────

    /** "All" | "Unread" | "Mentioned" */
    private val _activeTab = MutableStateFlow("All")
    val activeTab: StateFlow<String> = _activeTab

    fun setTab(tab: String) {
        _activeTab.value = tab
        applyFilter()
    }

    private var _allNotifications: List<Notification> = emptyList()

    private fun applyFilter() {
        _notifications.value = when (_activeTab.value) {
            "Unread"    -> _allNotifications.filter { !it.is_read }
            "Mentioned" -> _allNotifications.filter { it.type == "comment" }
            else        -> _allNotifications
        }
    }

    // ── Polling automático ───────────────────────────────────────────────────

    /**
     * Job de polling activo — cancelado automaticamente pelo viewModelScope
     * quando o ViewModel é destruído.
     */
    private var pollingJob: Job? = null

    /**
     * Carrega as notificações uma vez e arranca o polling a cada 30 s.
     * Idempotente — chamadas repetidas não criam jobs duplicados.
     */
    fun load() {
        val userId = SessionManager.userId
        if (userId < 0) return

        // Já tem polling activo — não duplicar
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            // Primeira carga com indicador de loading
            _isLoading.value = true
            fetchAndUpdate(userId)
            _isLoading.value = false

            // Polling silencioso a cada 30 segundos
            while (isActive) {
                delay(30_000L)
                fetchAndUpdate(userId)
            }
        }
    }

    private suspend fun fetchAndUpdate(userId: Long) {
        val fetched = repo.getNotificationsForUser(userId)
        _allNotifications = fetched
        applyFilter()
        _unreadCount.value = fetched.count { !it.is_read }
    }

    // ── Ações ────────────────────────────────────────────────────────────────

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            if (repo.markAsRead(notificationId)) {
                _allNotifications = _allNotifications.map {
                    if (it.id == notificationId) it.copy(is_read = true) else it
                }
                applyFilter()
                _unreadCount.value = _allNotifications.count { !it.is_read }
            }
        }
    }

    fun markAllAsRead() {
        val userId = SessionManager.userId
        if (userId < 0) return
        viewModelScope.launch {
            if (repo.markAllAsRead(userId)) {
                _allNotifications = _allNotifications.map { it.copy(is_read = true) }
                applyFilter()
                _unreadCount.value = 0
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}