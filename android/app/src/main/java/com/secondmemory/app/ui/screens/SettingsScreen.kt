@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.secondmemory.app.domain.Appearance
import com.secondmemory.app.domain.FREE_ACTIVE_LIMIT
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.activeCount
import com.secondmemory.app.notify.NotificationHelper

@Composable
fun SettingsScreen(
    settings: Settings,
    things: List<Thing>,
    onPatch: ((Settings) -> Settings) -> Unit,
    onLoadExamples: () -> Unit,
    onReset: () -> Unit,
    onExport: (Uri, String?) -> Unit,
    onImport: (Uri, String?) -> Unit,
) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onPatch { it.copy(notificationsEnabled = granted, notificationsAsked = true) }
    }
    var password by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExport(uri, password.takeIf { it.isNotBlank() })
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri, password.takeIf { it.isNotBlank() })
    }
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Pinned items stay in your notification shade until you unpin them. Later is the only schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        Label("Appearance")
        Row {
            Appearance.entries.forEach { appearance ->
                FilterChip(
                    selected = settings.appearance == appearance,
                    onClick = { onPatch { it.copy(appearance = appearance) } },
                    label = { Text(appearance.name.lowercase().replaceFirstChar { c -> c.titlecase() }) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        SettingSwitch("Show pins in the notification shade", settings.notificationsEnabled) { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onPatch { it.copy(notificationsEnabled = enabled, notificationsAsked = true) }
            }
        }
        SettingSwitch("Hide pin text on the lock screen", settings.lockScreenPrivate) {
            onPatch { s -> s.copy(lockScreenPrivate = it) }
        }
        TextButton(onClick = { NotificationHelper.showWelcome(context) }) {
            Text("Send a test pin")
        }
        SettingSwitch("Automatic titles", settings.automaticProcessing) {
            onPatch { s -> s.copy(automaticProcessing = it) }
        }

        Label("Auto-expire pins")
        Row {
            listOf(0 to "Never", 24 to "1 day", 72 to "3 days", 168 to "1 week").forEach { (hours, label) ->
                FilterChip(
                    selected = settings.pinExpiryHours == hours,
                    onClick = { onPatch { it.copy(pinExpiryHours = hours) } },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        SettingSwitch("Unlock unlimited pins", settings.isPro) {
            onPatch { s -> s.copy(isPro = it) }
        }

        val active = activeCount(things)
        Text(
            if (settings.isPro) "Unlimited active things"
            else "Free · $active / $FREE_ACTIVE_LIMIT active things",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp),
        )

        Label("Backup")
        Text(
            "Export everything as a zip. Set a passphrase to encrypt it (AES-256). Same passphrase to restore.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Optional passphrase") },
            singleLine = true,
        )
        TextButton(onClick = { exporter.launch("second-memory-backup.zip") }) {
            Text("Export backup")
        }
        TextButton(onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) {
            Text("Restore backup")
        }

        TextButton(onClick = onLoadExamples, modifier = Modifier.padding(top = 12.dp)) {
            Text("Load example things")
        }
        TextButton(onClick = { confirmReset = true }) {
            Text("Reset this device")
        }
        Text(
            "Share a link, photo, PDF, video, audio, contact or file from any app. Checklists in notes use [ ] lines. Add the pin widget or Quick Settings tile from the home screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset this device?") },
            text = { Text("Deletes every saved thing on this phone. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
