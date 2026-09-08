@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.ContentType
import com.secondmemory.app.domain.SnoozeOption
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.domain.categoryLabel
import com.secondmemory.app.domain.thingActionVerb
import com.secondmemory.app.ui.theme.Forest
import com.secondmemory.app.ui.theme.ForestOn
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThingCard(
    thing: Thing,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    onSnooze: (Long) -> Unit,
    onArchive: () -> Unit,
    onPin: () -> Unit,
    snoozeOptions: List<SnoozeOption>,
    showResurface: Boolean = true,
) {
    var menu by remember { mutableStateOf(false) }
    val closed = thing.status == ThingStatus.COMPLETED || thing.status == ThingStatus.ARCHIVED
    val haptic = LocalHapticFeedback.current
    val laterAt = snoozeOptions.firstOrNull { it.id == "tonight" }?.at
        ?: snoozeOptions.firstOrNull()?.at

    val body: @Composable () -> Unit = {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    CategoryDot(thing.category)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                        Text(
                            text = thing.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val meta = listOfNotNull(
                            if (thing.isPinned || thing.isFavourite) "Pinned" else null,
                            categoryLabel(thing.category),
                            thing.siteName ?: hostOf(thing.sourceUrl) ?: thing.sourceApp,
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
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
                    } else if (thing.contentType == ContentType.PDF ||
                        thing.contentType == ContentType.VIDEO ||
                        thing.contentType == ContentType.AUDIO ||
                        thing.contentType == ContentType.FILE
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                when (thing.contentType) {
                                    ContentType.PDF -> "PDF"
                                    ContentType.VIDEO -> "Video"
                                    ContentType.AUDIO -> "Audio"
                                    else -> "File"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                if (!closed) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDone) {
                            Text(thingActionVerb(thing))
                        }
                        TextButton(onClick = onPin) {
                            Text(if (thing.isPinned || thing.isFavourite) "Unpin" else "Pin to Today")
                        }
                        Spacer(Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Outlined.MoreHoriz, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                laterAt?.let { at ->
                                    DropdownMenuItem(
                                        text = { Text("Later tonight") },
                                        onClick = { menu = false; onSnooze(at) },
                                    )
                                }
                                DropdownMenuItem(text = { Text("Archive") }, onClick = { menu = false; onArchive() })
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onPin) {
                            Text("Back to Today")
                        }
                    }
                }
            }
        }
    }

    if (closed) {
        body()
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.32f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDone()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPin()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val target = dismissState.targetValue
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Forest)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = ForestOn)
                        Text(
                            "Done",
                            color = ForestOn,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFF8C5A2B))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Unpin",
                            color = ForestOn,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = ForestOn)
                    }
                }
            }
            if (target == SwipeToDismissBoxValue.Settled) {
                // Keep both hints visible so the gesture is learnable.
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        content = { body() },
    )
}

@Composable
fun CategoryDot(category: Category) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(categoryColor(category)),
    )
}

fun categoryColor(category: Category) = when (category) {
    Category.READ -> Forest
    Category.WATCH -> androidx.compose.ui.graphics.Color(0xFF3D5A80)
    Category.BUY -> androidx.compose.ui.graphics.Color(0xFF8C5A2B)
    Category.DO, Category.WORK -> androidx.compose.ui.graphics.Color(0xFF8F3D32)
    Category.EVENT -> androidx.compose.ui.graphics.Color(0xFF5C4A8A)
    Category.RECIPE -> androidx.compose.ui.graphics.Color(0xFF6B7C3A)
    Category.PLACE -> androidx.compose.ui.graphics.Color(0xFF2F6F6A)
    Category.IDEA -> androidx.compose.ui.graphics.Color(0xFF7A5C2E)
    else -> androidx.compose.ui.graphics.Color(0xFF6A655C)
}

fun hostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        java.net.URI(url).host?.removePrefix("www.")
    } catch (_: Exception) {
        null
    }
}

fun compactFuture(at: Long): String {
    val now = System.currentTimeMillis()
    val delta = at - now
    if (delta < 60_000) return "now"
    if (delta < 3600_000) return "in ${delta / 60_000}m"
    if (delta < 24 * 3600_000) return "in ${delta / 3600_000}h"
    val fmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    return fmt.format(Date(at))
}

fun relativeAge(at: Long): String {
    val delta = System.currentTimeMillis() - at
    return when {
        delta < 60_000 -> "just now"
        delta < 3600_000 -> "${delta / 60_000}m ago"
        delta < 24 * 3600_000 -> "${delta / 3600_000}h ago"
        else -> "${delta / (24 * 3600_000)}d ago"
    }
}
