package com.secondmemory.app.notify

import android.content.Context
import com.secondmemory.app.data.MemoryRepository
import com.secondmemory.app.widget.PinWidget

object ShadeSync {
    suspend fun refresh(
        context: Context,
        repo: MemoryRepository,
        restoreMissing: Boolean = false,
    ) {
        val settings = repo.currentSettings()
        val things = repo.currentThings()
        if (!settings.notificationsEnabled) {
            NotificationHelper.clear(context)
        } else {
            NotificationHelper.refreshPins(context, things, settings, restoreMissing)
        }
        ReminderScheduler.scheduleNext(context, things)
        PinWidget.update(context, things)
    }
}
