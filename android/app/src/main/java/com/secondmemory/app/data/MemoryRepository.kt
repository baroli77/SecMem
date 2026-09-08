package com.secondmemory.app.data

import com.secondmemory.app.domain.ActivityEvent
import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.domain.CaptureResult
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.FREE_ACTIVE_LIMIT
import com.secondmemory.app.domain.Heuristics
import com.secondmemory.app.domain.Priority
import com.secondmemory.app.domain.ProcessingStatus
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Seed
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.domain.activeCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class MemoryRepository(
    private val dao: ThingDao,
    private val settingsStore: SettingsStore,
) {
    val things: Flow<List<Thing>> = dao.observeThings().map { list -> list.map { it.toDomain() } }
    val activities: Flow<List<ActivityEvent>> = dao.observeActivities().map { list -> list.map { it.toDomain() } }
    val settings: Flow<Settings> = settingsStore.settings

    fun observeThing(id: String): Flow<Thing?> = dao.observeThing(id).map { it?.toDomain() }

    suspend fun currentSettings(): Settings = settings.first()
    suspend fun currentThings(): List<Thing> = dao.getThings().map { it.toDomain() }

    suspend fun capture(input: CaptureInput): CaptureResult {
        val settings = currentSettings()
        val parsed = Heuristics.parseCaptureInput(input)
        if (!input.forceDuplicate) {
            val duplicate = Heuristics.findDuplicate(currentThings(), parsed.sourceUrl)
            if (duplicate != null) {
                val pinned = if (duplicate.isPinned) duplicate else {
                    duplicate.copy(isPinned = true, status = ThingStatus.ACTIVE, updatedAt = System.currentTimeMillis())
                        .also { dao.upsert(it.toEntity()) }
                }
                return CaptureResult(thing = pinned, duplicate = duplicate)
            }
        }
        if (!settings.isPro && activeCount(currentThings()) >= FREE_ACTIVE_LIMIT) {
            val first = currentThings().firstOrNull() ?: placeholderBlocked()
            return CaptureResult(thing = first, blocked = true)
        }

        val now = System.currentTimeMillis()
        var draft = Thing(
            id = Seed.nid(),
            createdAt = now,
            updatedAt = now,
            originalContent = parsed.originalContent,
            contentType = parsed.contentType,
            sourceUrl = parsed.sourceUrl,
            sourceApp = parsed.sourceApp ?: "Second Memory",
            title = parsed.title,
            summary = parsed.summary,
            imageUri = parsed.imageUri,
            mimeType = parsed.mimeType,
            category = parsed.category,
            status = ThingStatus.ACTIVE,
            isPinned = true,
            priority = if (parsed.dueAt != null) Priority.HIGH else Priority.NORMAL,
            dueAt = parsed.dueAt,
            tags = parsed.tags,
            detectedDate = parsed.detectedDate,
            detectedTime = parsed.detectedTime,
            detectedPerson = parsed.detectedPerson,
            processingStatus = if (settings.automaticProcessing) ProcessingStatus.QUEUED else ProcessingStatus.NONE,
        )
        dao.upsert(draft.toEntity())
        log("captured", draft)
        return CaptureResult(thing = draft)
    }

    suspend fun enrich(id: String) {
        val existing = dao.getThing(id)?.toDomain() ?: return
        val settings = currentSettings()
        dao.upsert(
            existing.copy(
                processingStatus = ProcessingStatus.PROCESSING,
                updatedAt = System.currentTimeMillis(),
            ).toEntity(),
        )
        var next = existing
        try {
            val url = existing.sourceUrl
            if (!url.isNullOrBlank()) {
                val meta = MetadataFetcher.fetch(url)
                if (meta != null) {
                    next = next.copy(
                        title = meta.title?.takeIf { it.length >= 3 } ?: next.title,
                        summary = meta.description ?: next.summary,
                        siteName = meta.siteName ?: next.siteName,
                        sourceUrl = meta.canonicalUrl ?: next.sourceUrl,
                    )
                    if (next.category == Category.UNKNOWN || next.category == Category.READ) {
                        val reclass = Heuristics.extractSignals(next.originalContent, next.sourceUrl)
                        if (reclass.category != Category.UNKNOWN) {
                            next = next.copy(category = reclass.category, tags = (next.tags + reclass.tags).distinct())
                        }
                    }
                }
            }
            next = next.copy(
                aiProcessed = true,
                aiProvider = "heuristic",
                aiConfidence = 0.7f,
                processingStatus = ProcessingStatus.COMPLETE,
                updatedAt = System.currentTimeMillis(),
            )
            dao.upsert(next.toEntity())
            log("processed", next)
        } catch (e: Exception) {
            dao.upsert(
                existing.copy(
                    processingStatus = ProcessingStatus.FAILED,
                    processingError = e.message,
                    updatedAt = System.currentTimeMillis(),
                ).toEntity(),
            )
            log("processing_failed", existing, e.message)
        }
    }

    suspend fun complete(id: String) = patch(id) {
        it.copy(
            status = ThingStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            isPinned = false,
            isFavourite = false,
            resurfaceAt = null,
        )
    }.also { log("completed", it) }

    suspend fun archive(id: String) = patch(id) {
        it.copy(
            status = ThingStatus.ARCHIVED,
            archivedAt = System.currentTimeMillis(),
            isPinned = false,
            isFavourite = false,
            resurfaceAt = null,
        )
    }.also { log("archived", it) }

    suspend fun restore(id: String) = patch(id) {
        it.copy(
            status = ThingStatus.ACTIVE,
            completedAt = null,
            archivedAt = null,
            isPinned = true,
            isFavourite = false,
        )
    }

    suspend fun setPinned(id: String, pinned: Boolean) = patch(id) {
        it.copy(
            isPinned = pinned,
            isFavourite = if (pinned) false else it.isFavourite,
            status = if (pinned && (it.status == ThingStatus.COMPLETED || it.status == ThingStatus.ARCHIVED)) {
                ThingStatus.ACTIVE
            } else {
                it.status
            },
            completedAt = if (pinned) null else it.completedAt,
            archivedAt = if (pinned) null else it.archivedAt,
        )
    }

    suspend fun snooze(id: String, until: Long) = patch(id) {
        it.copy(
            status = ThingStatus.ACTIVE,
            isPinned = false,
            completedAt = null,
            archivedAt = null,
            resurfaceAt = until,
            reasonForResurface = "Snoozed",
        )
    }.also { log("snoozed", it) }

    suspend fun keep(id: String) = setPinned(id, true)

    suspend fun remove(id: String) {
        val existing = dao.getThing(id)?.toDomain()
        dao.delete(id)
        if (existing != null) log("deleted", existing)
    }

    suspend fun togglePin(id: String): Thing? {
        val existing = dao.getThing(id)?.toDomain() ?: return null
        val currently = existing.isPinned || existing.isFavourite
        return setPinned(id, !currently)
    }
    suspend fun toggleFavourite(id: String) = patch(id) { it.copy(isFavourite = !it.isFavourite) }.also {
        if (it?.isFavourite == true) log("favourited", it)
    }

    suspend fun openThing(id: String) = patch(id) {
        it.copy(
            lastOpenedAt = System.currentTimeMillis(),
            status = if (it.status == ThingStatus.INBOX) ThingStatus.ACTIVE else it.status,
        )
    }.also { log("opened", it) }

    suspend fun updateNotes(id: String, notes: String) = patch(id) { it.copy(notes = notes) }
    suspend fun updateTitle(id: String, title: String) = patch(id) { it.copy(title = title) }
    suspend fun updateCategory(id: String, category: Category) = patch(id) {
        val settings = currentSettings()
        val suggestion = Resurface.suggestResurfaceAt(it.copy(category = category), settings)
        it.copy(category = category, resurfaceAt = suggestion.first, reasonForResurface = suggestion.second)
    }

    suspend fun markResurfaced(id: String) = patch(id) {
        it.copy(
            lastResurfacedAt = System.currentTimeMillis(),
            resurfaceCount = it.resurfaceCount + 1,
            status = if (it.status == ThingStatus.INBOX) ThingStatus.ACTIVE else it.status,
        )
    }.also { log("resurfaced", it) }

    suspend fun tickAndCollectDue(): List<Thing> {
        val due = Resurface.tick(currentThings())
        due.forEach { markResurfaced(it.id) }
        return due
    }

    suspend fun recordNudge(): Boolean {
        val settings = currentSettings()
        val (allowed, _) = Resurface.shouldNudge(settings)
        if (!allowed) return false
        val day = Resurface.formatDay(Calendar.getInstance())
        settingsStore.update {
            val used = if (it.nudgesOn == day) it.nudgesToday else 0
            it.copy(nudgesOn = day, nudgesToday = used + 1)
        }
        return true
    }

    suspend fun patchSettings(transform: (Settings) -> Settings) = settingsStore.update(transform)

    suspend fun completeOnboarding(loadExamples: Boolean = true) {
        settingsStore.update { it.copy(onboardingComplete = true) }
        if (loadExamples && currentThings().isEmpty()) {
            dao.upsertAll(Seed.examples().map { it.toEntity() })
        }
    }

    suspend fun loadExamples() {
        dao.upsertAll(Seed.examples().map { it.toEntity() })
    }

    suspend fun resetAll() {
        dao.deleteAll()
        dao.deleteActivities()
        settingsStore.update { Settings() }
    }

    private suspend fun patch(id: String, transform: suspend (Thing) -> Thing): Thing? {
        val existing = dao.getThing(id)?.toDomain() ?: return null
        val next = transform(existing).copy(updatedAt = System.currentTimeMillis())
        dao.upsert(next.toEntity())
        return next
    }

    private suspend fun log(type: String, thing: Thing?, detail: String? = null) {
        if (thing == null) return
        dao.insertActivity(
            ActivityEvent(
                id = UUID.randomUUID().toString(),
                at = System.currentTimeMillis(),
                type = type,
                thingId = thing.id,
                title = thing.title,
                detail = detail,
            ).toEntity(),
        )
    }

    private fun placeholderBlocked(): Thing = Thing(
        id = "blocked",
        createdAt = 0,
        updatedAt = 0,
        originalContent = "",
        contentType = com.secondmemory.app.domain.ContentType.TEXT,
        title = "Limit reached",
    )
}
