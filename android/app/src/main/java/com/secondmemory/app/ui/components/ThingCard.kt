@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secondmemory.app.domain.ContentType
import com.secondmemory.app.domain.SnoozeOption
import com.secondmemory.app.domain.Thing
import java.io.File

@Composable
fun ThingCard(
    thing: Thing,
    onOpen: () -> Unit,
    onSnooze: (Long) -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    snoozeOptions: List<SnoozeOption>,
) {
    var menu by remember { mutableStateOf(false) }
    var later by remember { mutableStateOf(false) }
    val pinned = thing.isPinned

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                    Text(
                        text = thing.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        if (pinned) "Pinned" else null,
                        thing.siteName ?: hostOf(thing.sourceUrl) ?: thing.sourceApp,
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!thing.summary.isNullOrBlank()) {
                        Text(
                            text = thing.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                if (thing.contentType == ContentType.IMAGE) {
                    thing.imageUri?.let { uri ->
                        AsyncImage(
                            model = File(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpen) { Text("Open") }
                if (pinned) {
                    Box {
                        TextButton(onClick = { later = true }) { Text("Later") }
                        DropdownMenu(expanded = later, onDismissRequest = { later = false }) {
                            snoozeOptions.take(3).forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = { later = false; onSnooze(opt.at) },
                                )
                            }
                        }
                    }
                    TextButton(onClick = onPin) { Text("Unpin") }
                } else {
                    TextButton(onClick = onPin) { Text("Pin") }
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
    }
}
