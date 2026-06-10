package com.example.forgeplan.notifications.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Notification
import com.example.forgeplan.notifications.viewmodel.NotificationViewModel

// ── Strings i18n ────────────────────────────────────────────────────────────

private object Strings {
    val title         get() = appText("Notifications", "Notificações")
    val tabAll        get() = appText("All", "Todos")
    val tabUnread     get() = appText("Unread", "Não lido")
    val tabMentioned  get() = appText("Mentioned", "Mencionado")
    val markAllRead   get() = appText("Mark all as read", "Marcar tudo como lido")
    val today         get() = appText("Today", "Hoje")
    val yesterday     get() = appText("Yesterday", "Ontem")
    val thisWeek      get() = appText("This week", "Esta semana")
    val older         get() = appText("Older", "Mais antigo")
    val empty         get() = appText("No notifications", "Sem notificações")
    val emptyHint     get() = appText("You're all caught up!", "Está tudo em dia!")
    val minAgo        get() = appText("min ago", "min atrás")
    val hAgo          get() = appText("h ago", "h atrás")
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit = {},
    vm: NotificationViewModel = viewModel()
) {
    val notifications by vm.notifications.collectAsState()
    val activeTab     by vm.activeTab.collectAsState()
    val isLoading     by vm.isLoading.collectAsState()
    val isLandscape   =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            NotificationTopBar(
                onBack = onBack,
                onMarkAll = { vm.markAllAsRead() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab row
            NotificationTabs(activeTab = activeTab, onTabSelected = { vm.setTab(it) })

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (notifications.isEmpty()) {
                NotificationEmptyState()
            } else {
                val grouped = groupNotifications(notifications)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = if (isLandscape) 48.dp else 0.dp,
                        vertical = 8.dp
                    )
                ) {
                    grouped.forEach { (section, items) ->
                        item {
                            Text(
                                text = section,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(items, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onClick = { vm.markAsRead(notif.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── TopBar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(onBack: () -> Unit, onMarkAll: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = Strings.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onMarkAll) {
                Text(
                    text = Strings.markAllRead,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// ── Tabs ─────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationTabs(activeTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf(
        "All"      to Strings.tabAll,
        "Unread"   to Strings.tabUnread,
        "Mentioned" to Strings.tabMentioned
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (key, label) ->
            val selected = activeTab == key
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(key) }
            ) {
                Text(
                    text = label,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ── Notification Card ─────────────────────────────────────────────────────────

@Composable
private fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    val icon   = notificationIcon(notification.type)
    val color  = notificationColor(notification.type)
    val isRead = notification.is_read

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isRead) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isRead) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Ícone com fundo colorido
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Indicador não lido à esquerda do título
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = notification.title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!isRead) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(2.dp))

                notification.message?.let {
                    Text(
                        text = "\"$it\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    text = formatRelativeTime(notification.created_at) +
                            (notification.project_id?.let { " • Project $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }

            // Ponto vermelho de não lido no canto superior direito
            if (!isRead) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun NotificationEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = Strings.empty,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = Strings.emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun notificationIcon(type: String?): ImageVector = when (type) {
    "new_task"      -> Icons.Default.NewReleases
    "deadline"      -> Icons.Default.Warning
    "status_change" -> Icons.Default.Sync
    "comment"       -> Icons.Default.Comment
    "completion"    -> Icons.Default.CheckCircle
    else            -> Icons.Default.Schedule
}

private fun notificationColor(type: String?): Color = when (type) {
    "new_task"      -> Color(0xFF5C6BC0)   // índigo
    "deadline"      -> Color(0xFFE53935)   // vermelho
    "status_change" -> Color(0xFF43A047)   // verde
    "comment"       -> Color(0xFF8D6E63)   // castanho
    "completion"    -> Color(0xFF00ACC1)   // ciano
    else            -> Color(0xFF78909C)   // cinzento
}

/** Agrupa notificações em secções: Hoje / Ontem / Esta semana / Mais antigo */
private fun groupNotifications(
    list: List<Notification>
): LinkedHashMap<String, List<Notification>> {
    // Sem acesso ao relógio real no modelo, agrupamos pelos primeiros caracteres de created_at.
    // Numa implementação real, calcular-se-ia a diferença em dias com LocalDate.
    val today     = java.time.LocalDate.now().toString()
    val yesterday = java.time.LocalDate.now().minusDays(1).toString()
    val weekAgo   = java.time.LocalDate.now().minusDays(7).toString()

    val grouped = LinkedHashMap<String, List<Notification>>()

    fun label(date: String?): String {
        if (date == null) return "older"
        val d = date.take(10)
        return when {
            d == today     -> "today"
            d == yesterday -> "yesterday"
            d >= weekAgo   -> "week"
            else           -> "older"
        }
    }

    val byLabel = list.groupBy { label(it.created_at) }
    if (byLabel["today"]     != null) grouped[todayLabel()]     = byLabel["today"]!!
    if (byLabel["yesterday"] != null) grouped[yesterdayLabel()] = byLabel["yesterday"]!!
    if (byLabel["week"]      != null) grouped[weekLabel()]      = byLabel["week"]!!
    if (byLabel["older"]     != null) grouped[olderLabel()]     = byLabel["older"]!!
    return grouped
}

// Funções auxiliares para labels com i18n:
private fun todayLabel()     = appText("Today", "Hoje")
private fun yesterdayLabel() = appText("Yesterday", "Ontem")
private fun weekLabel()      = appText("This week", "Esta semana")
private fun olderLabel()     = appText("Older", "Mais antigo")

private fun formatRelativeTime(createdAt: String?): String {
    if (createdAt == null) return ""
    return try {
        val instant = java.time.Instant.parse(createdAt)
        val now     = java.time.Instant.now()
        val diffMin = java.time.Duration.between(instant, now).toMinutes()
        when {
            diffMin < 60   -> "$diffMin ${appText("min ago", "min atrás")}"
            diffMin < 1440 -> "${diffMin / 60} ${appText("h ago", "h atrás")}"
            else           -> {
                val days = diffMin / 1440
                "$days ${appText("days ago", "dias atrás")}"
            }
        }
    } catch (e: Exception) {
        createdAt.take(10)
    }
}