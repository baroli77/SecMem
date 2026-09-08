package com.secondmemory.app.ui

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    fun remove(id: String) = viewModelScope.launch { repo.remove(id) }
    fun togglePin(id: String) = viewModelScope.launch { repo.togglePin(id) }
    fun toggleFavourite(id: String) = viewModelScope.launch { repo.toggleFavourite(id) }
    fun openThing(id: String) = viewModelScope.launch { repo.openThing(id) }
    fun updateNotes(id: String, notes: String) = viewModelScope.launch { repo.updateNotes(id, notes) }
    fun updateTitle(id: String, title: String) = viewModelScope.launch { repo.updateTitle(id, title) }
    fun updateCategory(id: String, category: Category) = viewModelScope.launch { repo.updateCategory(id, category) }
    fun patchSettings(transform: (Settings) -> Settings) = viewModelScope.launch { repo.patchSettings(transform) }
    fun completeOnboarding() = viewModelScope.launch { repo.completeOnboarding(loadExamples = true) }
    fun loadExamples() = viewModelScope.launch { repo.loadExamples() }
    fun resetAll() = viewModelScope.launch { repo.resetAll() }

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
