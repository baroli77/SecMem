package com.secondmemory.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.secondmemory.app.SecondMemoryApp
import java.util.concurrent.TimeUnit

class ResurfaceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SecondMemoryApp ?: return Result.success()
        val repo = app.container.repository
        repo.expireDuePins()
        val due = repo.tickAndCollectDue()
        due.forEach { repo.setPinned(it.id, true) }
        ShadeSync.refresh(applicationContext, repo)
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "resurface-tick"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResurfaceWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
