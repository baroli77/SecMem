package com.secondmemory.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.Checklist
import com.secondmemory.app.domain.PinStyle
import com.secondmemory.app.domain.Priority
import kotlinx.coroutines.delay
import com.secondmemory.app.domain.Heuristics
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.domain.categoryLabel
import com.secondmemory.app.domain.thingActionVerb
import com.secondmemory.app.ui.components.compactFuture
import com.secondmemory.app.ui.components.hostOf
import com.secondmemory.app.ui.components.relativeAge
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    thing: Thing?,
    settings: Settings,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    onSnooze: (Long) -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onNotes: (String) -> Unit,
    onTitle: (String) -> Unit,
    onCategory: (Category) -> Unit,
    onChecklist: (String) -> Unit = {},
    onColor: (String) -> Unit = {},
    onPriority: (Priority) -> Unit = {},
    onExpires: (Long?) -> Unit = {},
) {
    LaunchedEffect(thing?.id) { if (thing != null) onOpen() }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    if (thing == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("This thing is gone.")
            Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
        }
        return
    }
    var notes by remember(thing.id) { mutableStateOf(thing.notes.orEmpty()) }
    var editingTitle by remember(thing.id) { mutableStateOf(false) }
    var title by remember(thing.id) { mutableStateOf(thing.title) }
    var laterOpen by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var newCheck by remember { mutableStateOf("") }
    LaunchedEffect(notes) {
        delay(450)
        if (notes != thing.notes.orEmpty()) onNotes(notes)
    }
    LaunchedEffect(title) {
        delay(450)
        if (editingTitle && title != thing.title && title.isNotBlank()) onTitle(title)
    }
    val snooze = remember(settings) { Resurface.snoozeOptions(settings = settings) }
    val closed = thing.status == ThingStatus.COMPLETED || thing.status == ThingStatus.ARCHIVED
    val body = displayBody(thing)
    val source = hostOf(thing.sourceUrl) ?: thing.siteName ?: thing.sourceApp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        thing.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            if (editingTitle) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TextButton(onClick = { editingTitle = false }) { Text("Done editing") }
            } else {
                Text(
                    thing.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { editingTitle = true }) { Text("Edit title") }
            }

            Text(
                listOfNotNull(
                    categoryLabel(thing.category),
                    source,
                    relativeAge(thing.createdAt),
                    thing.resurfaceAt?.let { "back ${compactFuture(it)}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (thing.contentType == com.secondmemory.app.domain.ContentType.IMAGE) {
                thing.imageUri?.let { uri ->
                    AsyncImage(
                        model = File(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            }

            if (!body.isNullOrBlank()) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (closed) {
                    Button(onClick = onRestore) { Text("Back to Today") }
                } else {
                    Button(onClick = { onDone(); onBack() }) { Text(thingActionVerb(thing)) }
                    OutlinedButton(onClick = onPin) { Text(if (thing.isPinned || thing.isFavourite) "Unpin" else "Pin to Today") }
                    Box {
                        OutlinedButton(onClick = { laterOpen = true }) { Text("Later") }
                        DropdownMenu(expanded = laterOpen, onDismissRequest = { laterOpen = false }) {
                            snooze.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        laterOpen = false
                                        onSnooze(opt.at)
                                        scope.launch { snackbar.showSnackbar("I’ll bring this back · ${opt.label.lowercase()}") }
                                    },
                                )
                            }
                        }
                    }
                }
                val canOpen = !thing.sourceUrl.isNullOrBlank() ||
                    thing.imageUri?.let { java.io.File(it).exists() } == true
                if (canOpen) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                com.secondmemory.app.notify.NotificationHelper.openThing(context, thing)
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Open")
                    }
                }
            }

            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onArchive) { Text("Archive") }
                OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
            }

            val checks = remember(thing.checklist, thing.notes) {
                Checklist.parse(thing.checklist).ifEmpty { Checklist.fromNotes(thing.notes) }
            }
            Text("Checklist", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp))
            checks.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = item.done,
                        onCheckedChange = { onChecklist(Checklist.format(Checklist.toggle(checks, index))) },
                    )
                    Text(item.text)
                }
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCheck,
                    onValueChange = { newCheck = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Add item") },
                    singleLine = true,
                )
                TextButton(
                    onClick = {
                        if (newCheck.isNotBlank()) {
                            onChecklist(
                                Checklist.format(
                                    checks + com.secondmemory.app.domain.CheckItem(newCheck.trim(), false),
                                ),
                            )
                            newCheck = ""
                        }
                    },
                ) { Text("Add") }
            }
            Text("Colour", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PinStyle.colors.forEach { color ->
                    FilterChip(
                        selected = thing.pinColor == color,
                        onClick = { onColor(color) },
                        label = { Text(PinStyle.label(color)) },
                    )
                }
            }
            Text("Priority", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = thing.priority == p,
                        onClick = { onPriority(p) },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.titlecase() }) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                label = { Text("Notes") },
                minLines = 3,
            )

            TextButton(
                onClick = { showAdjust = !showAdjust },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (showAdjust) "Hide category" else "Change category")
            }
            if (showAdjust) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = thing.category == cat,
                            onClick = { onCategory(cat) },
                            label = { Text(categoryLabel(cat)) },
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this?") },
            text = { Text("It will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private fun displayBody(thing: Thing): String? {
    val summary = thing.summary?.trim().orEmpty()
    val raw = thing.originalContent.trim()
    val title = thing.title.trim()
    if (summary.isNotEmpty() && summary != title) return summary
    val stripped = Heuristics.truncate(
        raw.replace(Regex("""https?://\S+"""), "").replace(Regex("""\s+"""), " ").trim(),
        280,
    )
    if (stripped.isEmpty() || stripped == title) return null
    return stripped
}
