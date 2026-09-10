package com.secondmemory.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secondmemory.app.data.MemoryRepository
import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.domain.CaptureResult
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.notify.ShadeSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemoryViewModel(private val repo: MemoryRepository) : ViewModel() {
    private val _settingsReady = MutableStateFlow(false)
    val settingsReady: StateFlow<Boolean> = _settingsReady.asStateFlow()

    val things: StateFlow<List<Thing>> = repo.things.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val settings: StateFlow<Settings> = repo.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings(),
    )

    init {
        viewModelScope.launch {
            repo.settings.first()
            _settingsReady.value = true
        }
    }

    fun capture(input: CaptureInput, onResult: (CaptureResult) -> Unit) {
        viewModelScope.launch {
            val result = repo.capture(input)
            onResult(result)
            val saved = result.saved
            if (saved != null && result.duplicate == null) {
                repo.enrich(saved.id)
            }
        }
    }

    fun complete(id: String) = viewModelScope.launch { repo.complete(id) }
    fun archive(id: String) = viewModelScope.launch { repo.archive(id) }
    fun restore(id: String) = viewModelScope.launch { repo.restore(id) }
    fun snooze(id: String, until: Long) = viewModelScope.launch { repo.snooze(id, until) }
    fun keep(id: String) = viewModelScope.launch { repo.keep(id) }
    private val detached = mutableMapOf<String, Thing>()

    fun remove(id: String) = viewModelScope.launch { repo.remove(id) }

    fun detach(id: String) = viewModelScope.launch {
        repo.detach(id)?.let { detached[id] = it }
    }

    fun undoDetach(id: String) = viewModelScope.launch {
        detached.remove(id)?.let { repo.reinsert(it) }
    }

    fun purgeDetach(id: String) = viewModelScope.launch {
        val gone = detached.remove(id) ?: return@launch
        com.secondmemory.app.data.CaptureFiles.delete(gone.imageUri)
    }
    fun togglePin(id: String) = viewModelScope.launch { repo.togglePin(id) }
    fun openThing(id: String) = viewModelScope.launch { repo.openThing(id) }
    fun updateNotes(id: String, notes: String) = viewModelScope.launch { repo.updateNotes(id, notes) }
    fun updateTitle(id: String, title: String) = viewModelScope.launch { repo.updateTitle(id, title) }
    fun updateCategory(id: String, category: Category) = viewModelScope.launch { repo.updateCategory(id, category) }
    fun patchSettings(transform: (Settings) -> Settings) = viewModelScope.launch { repo.patchSettings(transform) }
    fun completeOnboarding() = viewModelScope.launch { repo.completeOnboarding(loadExamples = false) }
    fun loadExamples() = viewModelScope.launch { repo.loadExamples() }
    fun resetAll() = viewModelScope.launch { repo.resetAll() }
    fun setChecklist(id: String, raw: String) = viewModelScope.launch { repo.setChecklist(id, raw) }
    fun setPinColor(id: String, color: String) = viewModelScope.launch { repo.setPinColor(id, color) }
    fun setPriority(id: String, priority: com.secondmemory.app.domain.Priority) =
        viewModelScope.launch { repo.setPriority(id, priority) }
    fun setExpiresAt(id: String, at: Long?) = viewModelScope.launch { repo.setExpiresAt(id, at) }
    fun movePin(id: String, delta: Int) = viewModelScope.launch { repo.movePin(id, delta) }
    fun syncShade(context: Context, restoreMissing: Boolean = false) =
        viewModelScope.launch { ShadeSync.refresh(context, repo, restoreMissing) }

    fun exportBackup(
        context: Context,
        uri: android.net.Uri,
        password: String?,
        onDone: (Boolean, String) -> Unit,
    ) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val json = com.secondmemory.app.data.Backup.snapshot(things.value, settings.value)
                val bytes = com.secondmemory.app.data.Backup.pack(context, json, password)
                com.secondmemory.app.data.Backup.write(context, uri, bytes)
            }
        }.onSuccess { onDone(true, if (password.isNullOrBlank()) "Backup saved" else "Encrypted backup saved") }
            .onFailure { onDone(false, it.message ?: "Couldn’t export") }
    }

    fun importBackup(
        context: Context,
        uri: android.net.Uri,
        password: String?,
        onDone: (Boolean, String) -> Unit,
    ) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val bytes = com.secondmemory.app.data.Backup.read(context, uri)
                val (json, files) = com.secondmemory.app.data.Backup.unpack(bytes, password)
                val count = repo.importSnapshot(json, files)
                ShadeSync.refresh(context, repo, restoreMissing = true)
                count
            }
        }.onSuccess { count ->
            onDone(true, if (count == 1) "Restored 1 item" else "Restored $count items")
        }
            .onFailure {
                val msg = when {
                    it.message?.contains("encrypted", true) == true -> "This backup is encrypted"
                    it.message?.contains("Wrong", true) == true ||
                        it.cause is javax.crypto.AEADBadTagException ||
                        it is javax.crypto.AEADBadTagException -> "Wrong passphrase"
                    else -> it.message ?: "Couldn’t restore"
                }
                onDone(false, msg)
            }
    }

    fun inbox(things: List<Thing>) = things.filter { it.status == ThingStatus.INBOX }
    fun library(things: List<Thing>, query: String = "", category: Category? = null): List<Thing> {
        val q = query.trim().lowercase()
        return things.filter { t ->
            (category == null || t.category == category) &&
                (q.isEmpty() || t.title.lowercase().contains(q) ||
                    t.originalContent.lowercase().contains(q) ||
                    t.tags.any { it.lowercase().contains(q) } ||
                    (t.summary?.lowercase()?.contains(q) == true))
        }
    }

    companion object {
        fun factory(repo: MemoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MemoryViewModel(repo) as T
        }
    }
}
