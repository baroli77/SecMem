package com.secondmemory.app

import android.app.Application
import com.secondmemory.app.data.AppDatabase
import com.secondmemory.app.data.CaptureFiles
import com.secondmemory.app.data.MemoryRepository
import com.secondmemory.app.data.SettingsStore
import com.secondmemory.app.notify.ResurfaceWorker
import com.secondmemory.app.notify.ShadeSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: AppDatabase = AppDatabase.create(app)
    val settingsStore = SettingsStore(app)
    val repository = MemoryRepository(
        database.thingDao(),
        settingsStore,
        CaptureFiles.dir(app),
    )
}

class SecondMemoryApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ResurfaceWorker.schedule(this)
        container.scope.launch {
            container.repository.failStaleProcessing()
            container.repository.assignMissingNotifIds()
            container.repository.pruneCaptureFiles()
            container.repository.expireDuePins()
            ShadeSync.refresh(this@SecondMemoryApp, container.repository, restoreMissing = true)
        }
    }
}
