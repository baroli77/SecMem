@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.domain.categoryLabel
import com.secondmemory.app.ui.components.ThingCard

@Composable
fun InboxScreen(
    things: List<Thing>,
    settings: Settings,
    onOpen: (String) -> Unit,
    onDone: (String) -> Unit,
    onSnooze: (String, Long) -> Unit,
    onArchive: (String) -> Unit,
    onPin: (String) -> Unit,
) {
    val inbox = things.filter { it.status == ThingStatus.INBOX }
    val snooze = Resurface.snoozeOptions(settings = settings)
    ThingList(
        title = "Inbox",
        subtitle = if (inbox.isEmpty()) "Caught up. New shares land here first."
        else "${inbox.size} waiting to be understood or acted on.",
        things = inbox,
        snooze = snooze,
        empty = "Share something from another app, or tap Save.",
        onOpen = onOpen, onDone = onDone, onSnooze = onSnooze,
        onArchive = onArchive, onPin = onPin,
    )
}

@Composable
fun LibraryScreen(
    things: List<Thing>,
    settings: Settings,
    onOpen: (String) -> Unit,
    onDone: (String) -> Unit,
    onSnooze: (String, Long) -> Unit,
    onArchive: (String) -> Unit,
    onPin: (String) -> Unit,
) {
    var filter by remember { mutableStateOf<Category?>(null) }
    var status by remember { mutableStateOf("open") }
    val snooze = remember(settings) { Resurface.snoozeOptions(settings = settings) }
    val filtered = remember(things, status, filter) {
        things.filter { t ->
            val statusOk = when (status) {
                "open" -> t.status == ThingStatus.INBOX || t.status == ThingStatus.ACTIVE
                "done" -> t.status == ThingStatus.COMPLETED
                "archived" -> t.status == ThingStatus.ARCHIVED
                else -> true
            }
            statusOk && (filter == null || t.category == filter)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Library", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("open" to "Open", "done" to "Done", "archived" to "Archived").forEach { (id, label) ->
                FilterChip(selected = status == id, onClick = { status = id }, label = { Text(label) })
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
            Category.entries.forEach { cat ->
                FilterChip(
                    selected = filter == cat,
                    onClick = { filter = if (filter == cat) null else cat },
                    label = { Text(categoryLabel(cat)) },
                )
            }
        }
        ThingList(
            title = null,
            subtitle = null,
            things = filtered,
            snooze = snooze,
            empty = "Nothing in this filter yet.",
            onOpen = onOpen, onDone = onDone, onSnooze = onSnooze,
            onArchive = onArchive, onPin = onPin,
        )
    }
}

@Composable
fun SearchScreen(
    things: List<Thing>,
    settings: Settings,
    onOpen: (String) -> Unit,
    onDone: (String) -> Unit,
    onSnooze: (String, Long) -> Unit,
    onArchive: (String) -> Unit,
    onPin: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val snooze = remember(settings) { Resurface.snoozeOptions(settings = settings) }
    val q = query.trim().lowercase()
    val results = remember(things, q) {
        if (q.isEmpty()) emptyList() else things.filter {
            it.title.lowercase().contains(q) ||
                it.originalContent.lowercase().contains(q) ||
                (it.summary?.lowercase()?.contains(q) == true) ||
                it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Titles, people, links…") },
            singleLine = true,
        )
        ThingList(
            title = null,
            subtitle = null,
            things = results,
            snooze = snooze,
            empty = if (q.isEmpty()) "Type to find something you saved." else "No matches.",
            onOpen = onOpen, onDone = onDone, onSnooze = onSnooze,
            onArchive = onArchive, onPin = onPin,
        )
    }
}

@Composable
private fun ThingList(
    title: String?,
    subtitle: String?,
    things: List<Thing>,
    snooze: List<com.secondmemory.app.domain.SnoozeOption>,
    empty: String,
    onOpen: (String) -> Unit,
    onDone: (String) -> Unit,
    onSnooze: (String, Long) -> Unit,
    onArchive: (String) -> Unit,
    onPin: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            item {
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        "Swipe right to mark done. Swipe left to snooze until tonight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        if (things.isEmpty()) {
            item {
                Text(
                    empty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(things, key = { it.id }) { thing ->
            ThingCard(
                thing = thing,
                onOpen = { onOpen(thing.id) },
                onDone = { onDone(thing.id) },
                onSnooze = { onSnooze(thing.id, it) },
                onArchive = { onArchive(thing.id) },
                onPin = { onPin(thing.id) },
                snoozeOptions = snooze,
            )
        }
    }
}
