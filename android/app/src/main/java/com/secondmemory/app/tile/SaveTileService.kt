package com.secondmemory.app.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.secondmemory.app.MainActivity
import com.secondmemory.app.SecondMemoryApp
import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.notify.ShadeSync
import kotlinx.coroutines.launch

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
            startActivityAndCollapse(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("openCapture", true)
                },
            )
            return
        }
        val app = applicationContext as SecondMemoryApp
        app.container.scope.launch {
            app.container.repository.capture(CaptureInput(text = clip, sourceApp = "Quick Settings"))
            ShadeSync.refresh(applicationContext, app.container.repository)
        }
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Pinned"
            updateTile()
        }
    }
}
