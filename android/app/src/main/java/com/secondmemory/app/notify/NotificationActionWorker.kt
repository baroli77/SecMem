package com.secondmemory.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.secondmemory.app.SecondMemoryApp
import com.secondmemory.app.domain.Checklist
import com.secondmemory.app.domain.Resurface
import java.util.Calendar

class NotificationActionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SecondMemoryApp ?: return Result.success()
        val id = inputData.getString(NotificationHelper.EXTRA_THING_ID) ?: return Result.success()
        val action = inputData.getString(KEY_ACTION) ?: return Result.success()
        val repo = app.container.repository
        when (action) {
            NotificationHelper.ACTION_DONE -> repo.complete(id)
            NotificationHelper.ACTION_UNPIN -> repo.setPinned(id, false)
            NotificationHelper.ACTION_PIN -> repo.setPinned(id, true)
            NotificationHelper.ACTION_LATER -> {
                val until = Resurface.snoozeOptions(Calendar.getInstance()).firstOrNull { it.id == "tonight" }?.at
                    ?: Resurface.snoozeOptions(Calendar.getInstance()).first().at
                repo.snooze(id, until)
            }
            NotificationHelper.ACTION_CHECK -> {
                val thing = repo.currentThings().firstOrNull { it.id == id }
                if (thing != null) {
                    val items = Checklist.parse(thing.checklist).ifEmpty { Checklist.fromNotes(thing.notes) }
                    repo.setChecklist(id, Checklist.format(Checklist.checkNext(items)))
                }
            }
            NotificationHelper.ACTION_OPEN -> {
                val thing = repo.currentThings().firstOrNull { it.id == id }
                if (thing != null) NotificationHelper.openThing(applicationContext, thing)
            }
        }
        ShadeSync.refresh(applicationContext, repo)
        return Result.success()
    }

    companion object {
        const val KEY_ACTION = "action"

        fun enqueue(context: Context, action: String, thingId: String) {
            val work = OneTimeWorkRequestBuilder<NotificationActionWorker>()
                .setInputData(
                    workDataOf(
                        KEY_ACTION to action,
                        NotificationHelper.EXTRA_THING_ID to thingId,
                    ),
                )
                .build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
