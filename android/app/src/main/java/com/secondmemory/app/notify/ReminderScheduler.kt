package com.secondmemory.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.secondmemory.app.SecondMemoryApp
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST = 42

    fun scheduleNext(context: Context, things: List<Thing>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(context)
        am.cancel(pi)
        val now = System.currentTimeMillis()
        val nextAt = things
            .filter { it.status != ThingStatus.COMPLETED && it.status != ThingStatus.ARCHIVED }
            .filter { !NotificationHelper.isPinned(it) && it.reasonForResurface == "Snoozed" }
            .mapNotNull { it.resurfaceAt }
            .filter { it > now + 15_000L }
            .minOrNull() ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pi)
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as SecondMemoryApp
        app.container.scope.launch {
            try {
                val repo = app.container.repository
                val due = repo.tickAndCollectDue()
                val settings = repo.currentSettings()
                if (Resurface.isQuietHours(Calendar.getInstance(), settings)) {
                    ShadeSync.refresh(context, repo)
                    return@launch
                }
                due.forEach { repo.setPinned(it.id, true) }
                ShadeSync.refresh(context, repo)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        ResurfaceWorker.schedule(context)
        val pending = goAsync()
        val app = context.applicationContext as SecondMemoryApp
        app.container.scope.launch {
            try {
                ShadeSync.refresh(context, app.container.repository, restoreMissing = true)
            } finally {
                pending.finish()
            }
        }
    }
}
