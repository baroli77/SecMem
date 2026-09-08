package com.secondmemory.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.ui.components.ThingCard

@Composable
fun TodayScreen(
    things: List<Thing>,
    settings: Settings,
    onOpen: (String) -> Unit,
    onDone: (String) -> Unit,
    onSnooze: (String, Long) -> Unit,
    onArchive: (String) -> Unit,
    onPin: (String) -> Unit,
) {
    val pinned = remember(things) {
        things.filter {
            (it.isPinned || it.isFavourite) && it.status != ThingStatus.COMPLETED && it.status != ThingStatus.ARCHIVED
        }.sortedByDescending { it.updatedAt }
    }
    val snooze = remember(settings) { Resurface.snoozeOptions(settings = settings) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 8.dp, top = 4.dp)) {
                Text("Pinned", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (pinned.isEmpty()) "Nothing in the shade. Share something and it stays until you unpin it."
                    else "These stay in your notification shade until you unpin them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Swipe right to mark done. Swipe left to unpin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        items(pinned, key = { it.id }) { thing ->
            ThingCard(
                thing = thing,
                onOpen = { onOpen(thing.id) },
                onDone = { onDone(thing.id) },
                onSnooze = { onSnooze(thing.id, it) },
                onArchive = { onArchive(thing.id) },
                onPin = { onPin(thing.id) },
                snoozeOptions = snooze,
                showResurface = false,
            )
        }
    }
}
