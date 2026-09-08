@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.screens

import android.Manifest
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onPatch { it.copy(notificationsEnabled = granted, notificationsAsked = true) }
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
            "Pinned items stay in your notification shade until you unpin them. No schedule.",
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
        TextButton(onClick = { NotificationHelper.showWelcome(context) }) {
            Text("Send a test pin")
        }
        SettingSwitch("Automatic titles", settings.automaticProcessing) {
            onPatch { s -> s.copy(automaticProcessing = it) }
        }
        SettingSwitch("Pro (remove 40-item free limit)", settings.isPro) {
            onPatch { s -> s.copy(isPro = it) }
        }

        val active = activeCount(things)
        Text(
            if (settings.isPro) "Pro · unlimited active things"
            else "Free · $active / $FREE_ACTIVE_LIMIT active things",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp),
        )

        TextButton(onClick = onLoadExamples, modifier = Modifier.padding(top = 12.dp)) {
            Text("Load example things")
        }
        TextButton(onClick = onReset) {
            Text("Reset this device")
        }
        Text(
            "Share a link, photo, PDF, video, audio, contact or file from any app. It pins to your notification shade until you unpin it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
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
