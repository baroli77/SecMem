package com.secondmemory.app

import android.app.Application
import com.secondmemory.app.data.AppDatabase
import com.secondmemory.app.data.MemoryRepository
import com.secondmemory.app.data.SettingsStore
import com.secondmemory.app.notify.NotificationHelper
import com.secondmemory.app.notify.ReminderScheduler
import com.secondmemory.app.notify.ResurfaceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppContainer(app: Application) {
    val database: AppDatabase = AppDatabase.create(app)
    val settingsStore = SettingsStore(app)
    val repository = MemoryRepository(database.thingDao(), settingsStore)
}

class SecondMemoryApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        ResurfaceWorker.schedule(this)
        CoroutineScope(Dispatchers.IO).launch {
            val things = container.repository.currentThings()
            NotificationHelper.refreshPins(this@SecondMemoryApp, things)
            ReminderScheduler.scheduleNext(this@SecondMemoryApp, things)
        }
    }
}
