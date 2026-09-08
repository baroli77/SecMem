@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.ui.components.ThingCard

@Composable
fun LibraryScreen(
    things: List<Thing>,
    settings: Settings,
    onOpen: (String) -> Unit,
    onSnooze: (String, Long) -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val snooze = remember(settings) { Resurface.snoozeOptions(settings = settings) }
    val q = query.trim().lowercase()
    val saved = remember(things, q) {
        things.filter { !it.isPinned }.filter {
            q.isEmpty() ||
                it.title.lowercase().contains(q) ||
                it.originalContent.lowercase().contains(q) ||
                (it.summary?.lowercase()?.contains(q) == true) ||
                it.notes.orEmpty().lowercase().contains(q)
        }.sortedByDescending { it.updatedAt }
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Saved",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search") },
            singleLine = true,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (saved.isEmpty()) {
                item {
                    Text(
                        if (q.isEmpty()) "Unpinned things live here." else "No matches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            items(saved, key = { it.id }) { thing ->
                ThingCard(
                    thing = thing,
                    onOpen = { onOpen(thing.id) },
                    onSnooze = { onSnooze(thing.id, it) },
                    onDelete = { onDelete(thing.id) },
                    onPin = { onPin(thing.id) },
                    snoozeOptions = snooze,
                )
            }
        }
    }
}
