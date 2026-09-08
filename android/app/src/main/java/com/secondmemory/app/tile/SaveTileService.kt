package com.secondmemory.app.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.secondmemory.app.MainActivity
import com.secondmemory.app.SecondMemoryApp
import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.notify.ShadeSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(24)
class SaveTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Pin clipboard"
            updateTile()
        }
    }

    override fun onClick() {
        val clip = getSystemService(android.content.ClipboardManager::class.java)
            ?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if (clip.isBlank()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("openCapture", true)
            }
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        val app = applicationContext as SecondMemoryApp
        app.container.scope.launch {
            val result = app.container.repository.capture(CaptureInput(text = clip, sourceApp = "Quick Settings"))
            if (result.saved != null && result.duplicate == null) {
                app.container.repository.enrich(result.saved!!.id)
            }
            ShadeSync.refresh(applicationContext, app.container.repository)
            val msg = when {
                result.duplicate != null -> "Already saved"
                result.saved != null -> "Pinned"
                else -> "Couldn’t pin"
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                qsTile?.apply {
                    state = if (result.saved != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = if (result.saved != null && result.duplicate == null) "Pinned" else "Pin clipboard"
                    updateTile()
                }
            }
        }
    }
}
