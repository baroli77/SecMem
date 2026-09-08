package com.secondmemory.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.secondmemory.app.SecondMemoryApp
import com.secondmemory.app.domain.Resurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationHelper.EXTRA_THING_ID) ?: return
        val repo = (context.applicationContext as SecondMemoryApp).container.repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_DONE -> repo.complete(id)
                    NotificationHelper.ACTION_UNPIN -> repo.setPinned(id, false)
                    NotificationHelper.ACTION_PIN -> repo.setPinned(id, true)
                    NotificationHelper.ACTION_LATER -> {
                        val until = Resurface.snoozeOptions(Calendar.getInstance()).firstOrNull { it.id == "tonight" }?.at
                            ?: Resurface.snoozeOptions(Calendar.getInstance()).first().at
                        repo.snooze(id, until)
                    }
                    NotificationHelper.ACTION_OPEN -> {
                        val thing = repo.currentThings().firstOrNull { it.id == id }
                        if (thing != null) NotificationHelper.openThing(context, thing)
                    }
                }
                NotificationHelper.refreshPins(context, repo.currentThings())
            } finally {
                pending.finish()
            }
        }
    }
}
