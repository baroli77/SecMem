@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.secondmemory.app.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.secondmemory.app.data.Backup
import com.secondmemory.app.domain.Appearance
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.notify.NotificationHelper

@Composable
fun SettingsScreen(
    settings: Settings,
    onPatch: ((Settings) -> Settings) -> Unit,
    onLoadExamples: () -> Unit,
    onReset: () -> Unit,
    onExport: (Uri, String?) -> Unit,
    onImport: (Uri, String?) -> Unit,
) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onPatch { it.copy(notificationsEnabled = granted, notificationsAsked = true) }
    }
    var advanced by remember { mutableStateOf(false) }
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var needPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExport(uri, password.takeIf { encrypt && it.isNotBlank() })
        encrypt = false
        password = ""
        pendingAction = null
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching { Backup.read(context, uri) }.getOrNull()
        if (bytes != null && Backup.isEncrypted(bytes)) {
            pendingUri = uri
            restorePassword = ""
            needPassword = true
        } else if (uri != null) {
            onImport(uri, null)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            "Share it. It stays in the shade until you unpin it.",
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

        Label("Backup")
        TextButton(onClick = { pendingAction = "export"; exporter.launch("second-memory-backup.zip") }) {
            Text("Export backup")
        }
        TextButton(onClick = { pendingAction = "encrypt" }) {
            Text("Encrypted backup")
        }
        TextButton(onClick = { pendingAction = "import"; importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) {
            Text("Restore backup")
        }

        TextButton(onClick = { advanced = !advanced }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (advanced) "Hide advanced" else "Advanced")
        }
        if (advanced) {
            SettingSwitch("Automatic titles", settings.automaticProcessing) {
                onPatch { s -> s.copy(automaticProcessing = it) }
            }
            Label("Auto-expire pins")
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Never", 24 to "1 day", 72 to "3 days", 168 to "1 week").forEach { (hours, label) ->
                    FilterChip(
                        selected = settings.pinExpiryHours == hours,
                        onClick = { onPatch { it.copy(pinExpiryHours = hours) } },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "Applies to new pins only. Pins already in the shade are unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = onLoadExamples) { Text("Load example things") }
            TextButton(onClick = { NotificationHelper.showWelcome(context) }) { Text("Send a test pin") }
            Text(
                "Home screen: add the Pinned widget. Quick Settings: add the Pin clipboard tile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = { confirmReset = true }) { Text("Reset this device") }
        }

        Text(
            "Second Memory",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 28.dp, bottom = 32.dp),
        )
    }
    if (needPassword) {
        AlertDialog(
            onDismissRequest = { needPassword = false; pendingUri = null; restorePassword = "" },
            title = { Text("Encrypted backup") },
            text = {
                Column {
                    Text("This backup is encrypted. Enter the passphrase to restore it.")
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        label = { Text("Passphrase") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingUri
                        needPassword = false
                        pendingUri = null
                        if (uri != null) onImport(uri, restorePassword)
                        restorePassword = ""
                    },
                    enabled = restorePassword.length >= 4,
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { needPassword = false; pendingUri = null; restorePassword = "" }) { Text("Cancel") }
            },
        )
    }
    if (pendingAction == "encrypt") {
        AlertDialog(
            onDismissRequest = { pendingAction = null; password = ""; encrypt = false },
            title = { Text("Encrypt backup") },
            text = {
                Column {
                    Text("Choose a passphrase. You’ll need the same one to restore.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        label = { Text("Passphrase") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        encrypt = true
                        pendingAction = "export"
                        exporter.launch("second-memory-backup.zip")
                    },
                    enabled = password.length >= 4,
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null; password = "" }) { Text("Cancel") }
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset this device?") },
            text = { Text("Deletes every saved thing on this phone.") },
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
