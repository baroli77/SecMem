package com.secondmemory.app.data

import com.secondmemory.app.domain.ActivityEvent
import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.domain.CaptureResult
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.Heuristics
import com.secondmemory.app.domain.PinStyle
import com.secondmemory.app.domain.Priority
import com.secondmemory.app.domain.ProcessingStatus
import com.secondmemory.app.domain.Resurface
import com.secondmemory.app.domain.Seed
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Calendar
import java.util.UUID

class MemoryRepository(
    private val dao: ThingDao,
    private val settingsStore: SettingsStore,
    private val capturesDir: File,
) {
    private val write = Mutex()

    val things: Flow<List<Thing>> = dao.observeThings().map { list -> list.map { it.toDomain() } }
    val activities: Flow<List<ActivityEvent>> = dao.observeActivities().map { list -> list.map { it.toDomain() } }
    val settings: Flow<Settings> = settingsStore.settings

    fun observeThing(id: String): Flow<Thing?> = dao.observeThing(id).map { it?.toDomain() }

    suspend fun currentSettings(): Settings = settings.first()
    suspend fun currentThings(): List<Thing> = dao.getThings().map { it.toDomain() }

    suspend fun capture(input: CaptureInput): CaptureResult = write.withLock {
        val settings = currentSettings()
        val parsed = Heuristics.parseCaptureInput(input)
        if (!input.forceDuplicate) {
            val duplicate = Heuristics.findDuplicate(
                dao.thingsWithUrl().map { it.toDomain() },
                parsed.sourceUrl,
            )
            if (duplicate != null) {
                val restored = PinStyle.restoredDuplicate(duplicate)
                dao.upsert(restored.toEntity())
                return CaptureResult(thing = restored, duplicate = duplicate)
            }
        }

        val now = System.currentTimeMillis()
        val expiryHours = settings.pinExpiryHours
        val draft = Thing(
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
            notifId = dao.maxNotifId() + 1,
            sortOrder = dao.minSortOrder() - 1,
            expiresAt = if (expiryHours > 0) now + expiryHours * 3600_000L else null,
        )
        dao.upsert(draft.toEntity())
        log("captured", draft)
        return CaptureResult(thing = draft)
    }

    suspend fun enrich(id: String) {
        val existing = dao.getThing(id)?.toDomain() ?: return
        val settings = currentSettings()
        if (!settings.automaticProcessing || !settings.aiEnabled) return
        write.withLock {
            val latest = dao.getThing(id)?.toDomain() ?: return@withLock
            dao.upsert(
                latest.copy(
                    processingStatus = ProcessingStatus.PROCESSING,
                    updatedAt = System.currentTimeMillis(),
                ).toEntity(),
            )
        }
        try {
            var title = existing.title
            var summary = existing.summary
            var siteName = existing.siteName
            var sourceUrl = existing.sourceUrl
            var category = existing.category
            var tags = existing.tags
            var ogImage = existing.ogImageUrl
            val url = existing.sourceUrl
            if (!url.isNullOrBlank()) {
                val meta = MetadataFetcher.fetch(url)
                if (meta != null) {
                    title = meta.title?.takeIf { it.length >= 3 } ?: title
                    summary = meta.description ?: summary
                    siteName = meta.siteName ?: siteName
                    sourceUrl = MetadataFetcher.sanitize(meta.canonicalUrl ?: "")?.toString() ?: sourceUrl
                    ogImage = MetadataFetcher.sanitize(meta.image ?: "")?.toString() ?: ogImage
                    if (category == Category.UNKNOWN || category == Category.READ) {
                        val reclass = Heuristics.extractSignals(existing.originalContent, sourceUrl)
                        if (reclass.category != Category.UNKNOWN) {
                            category = reclass.category
                            tags = (tags + reclass.tags).distinct()
                        }
                    }
                }
            }
            write.withLock {
                val latest = dao.getThing(id)?.toDomain() ?: return@withLock
                val next = latest.copy(
                    title = title,
                    summary = summary,
                    siteName = siteName,
                    sourceUrl = sourceUrl,
                    category = category,
                    tags = tags,
                    ogImageUrl = ogImage,
                    aiProcessed = true,
                    aiProvider = "heuristic",
                    aiConfidence = 0.7f,
                    processingStatus = ProcessingStatus.COMPLETE,
                    updatedAt = System.currentTimeMillis(),
                )
                dao.upsert(next.toEntity())
                log("processed", next)
            }
        } catch (e: Exception) {
            write.withLock {
                val latest = dao.getThing(id)?.toDomain() ?: return@withLock
                dao.upsert(
                    latest.copy(
                        processingStatus = ProcessingStatus.FAILED,
                        processingError = e.message,
                        updatedAt = System.currentTimeMillis(),
                    ).toEntity(),
                )
                log("processing_failed", latest, e.message)
            }
        }
    }

    suspend fun mergeFavouritesIntoPins() {
        currentThings().filter { it.isFavourite && !it.isPinned }.forEach { setPinned(it.id, true) }
    }

    suspend fun failStaleProcessing() {
        dao.failStaleProcessing(System.currentTimeMillis() - 2 * 60_000L)
    }

    suspend fun assignMissingNotifIds() = write.withLock {
        var next = dao.maxNotifId()
        dao.missingNotifIds().forEach { row ->
            next += 1
            dao.upsert(row.copy(notifId = next))
        }
    }

    suspend fun pruneCaptureFiles() {
        CaptureFiles.pruneOrphans(capturesDir, dao.allImageUris().toSet())
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
        val now = System.currentTimeMillis()
        val settings = currentSettings()
        it.copy(
            isPinned = pinned,
            isFavourite = false,
            resurfaceAt = null,
            reasonForResurface = null,
            expiresAt = if (pinned && settings.pinExpiryHours > 0) {
                now + settings.pinExpiryHours * 3600_000L
            } else if (pinned) null else it.expiresAt,
            status = if (pinned && (it.status == ThingStatus.COMPLETED || it.status == ThingStatus.ARCHIVED)) {
                ThingStatus.ACTIVE
            } else {
                it.status
            },
            completedAt = if (pinned) null else it.completedAt,
            archivedAt = if (pinned) null else it.archivedAt,
            sortOrder = if (pinned) dao.minSortOrder() - 1 else it.sortOrder,
        )
    }

    suspend fun snooze(id: String, until: Long) = patch(id) {
        it.copy(
            status = ThingStatus.ACTIVE,
            isPinned = false,
            isFavourite = false,
            completedAt = null,
            archivedAt = null,
            resurfaceAt = until,
            reasonForResurface = "Snoozed",
        )
    }.also { log("snoozed", it) }

    suspend fun keep(id: String) = setPinned(id, true)

    suspend fun detach(id: String): Thing? {
        val existing = write.withLock {
            val row = dao.getThing(id)?.toDomain()
            dao.delete(id)
            row
        }
        if (existing != null) log("deleted", existing)
        return existing
    }

    suspend fun reinsert(thing: Thing) = write.withLock {
        dao.upsert(thing.toEntity())
    }

    suspend fun remove(id: String) {
        val existing = detach(id)
        CaptureFiles.delete(existing?.imageUri)
    }

    suspend fun togglePin(id: String): Thing? {
        val existing = dao.getThing(id)?.toDomain() ?: return null
        val currently = existing.isPinned
        return setPinned(id, !currently)
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
        it.copy(category = category)
    }

    suspend fun setImageUri(id: String, path: String) = patch(id) { it.copy(imageUri = path) }
    suspend fun setChecklist(id: String, raw: String) = patch(id) { it.copy(checklist = raw) }
    suspend fun setPinColor(id: String, color: String) = patch(id) { it.copy(pinColor = color) }
    suspend fun setPriority(id: String, priority: Priority) = patch(id) { it.copy(priority = priority) }
    suspend fun setExpiresAt(id: String, at: Long?) = patch(id) { it.copy(expiresAt = at) }
    suspend fun movePin(id: String, delta: Int) = patch(id) { it.copy(sortOrder = it.sortOrder + delta) }

    suspend fun expireDuePins(): Int {
        val now = System.currentTimeMillis()
        var n = 0
        currentThings().filter { it.isPinned && it.expiresAt != null && it.expiresAt <= now }.forEach {
            setPinned(it.id, false)
            n += 1
        }
        return n
    }

    suspend fun markResurfaced(id: String) = patch(id) {
        it.copy(
            lastResurfacedAt = System.currentTimeMillis(),
            resurfaceCount = it.resurfaceCount + 1,
            status = if (it.status == ThingStatus.INBOX) ThingStatus.ACTIVE else it.status,
        )
    }.also { log("resurfaced", it) }

    suspend fun tickAndCollectDue(): List<Thing> {
        val due = currentThings().filter {
            it.resurfaceAt != null &&
                it.resurfaceAt <= System.currentTimeMillis() &&
                it.reasonForResurface == "Snoozed" &&
                it.status != ThingStatus.COMPLETED &&
                it.status != ThingStatus.ARCHIVED &&
                !it.isPinned
        }
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

    suspend fun completeOnboarding(loadExamples: Boolean = false) {
        settingsStore.update { it.copy(onboardingComplete = true) }
        if (loadExamples && dao.getThings().isEmpty()) {
            dao.upsertAll(Seed.examples().map { it.toEntity() })
            assignMissingNotifIds()
        }
    }

    suspend fun loadExamples() {
        dao.upsertAll(Seed.examples().map { it.toEntity() })
        assignMissingNotifIds()
    }

    suspend fun resetAll() {
        write.withLock {
            dao.deleteAll()
            dao.deleteActivities()
        }
        CaptureFiles.deleteAll(capturesDir)
        settingsStore.update { Settings() }
    }

    suspend fun importSnapshot(json: org.json.JSONObject, files: Map<String, ByteArray>) {
        val arr = json.optJSONArray("things") ?: return
        val now = System.currentTimeMillis()
        val entities = mutableListOf<ThingEntity>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val oldPath = o.optString("imageUri").takeIf { it.isNotBlank() }
            val fileName = oldPath?.let { java.io.File(it).name }
            var imageUri = oldPath
            if (fileName != null && files.containsKey(fileName)) {
                val dest = java.io.File(capturesDir, fileName)
                dest.outputStream().use { it.write(files.getValue(fileName)) }
                imageUri = dest.absolutePath
            }
            entities += Thing(
                id = o.optString("id").ifBlank { Seed.nid() },
                createdAt = o.optLong("createdAt", now),
                updatedAt = o.optLong("updatedAt", now),
                originalContent = o.optString("originalContent"),
                contentType = runCatching { com.secondmemory.app.domain.ContentType.valueOf(o.optString("contentType")) }
                    .getOrDefault(com.secondmemory.app.domain.ContentType.TEXT),
                sourceUrl = o.optString("sourceUrl").takeIf { it.isNotBlank() },
                sourceApp = o.optString("sourceApp").takeIf { it.isNotBlank() },
                title = o.optString("title").ifBlank { "Imported" },
                summary = o.optString("summary").takeIf { it.isNotBlank() },
                notes = o.optString("notes").takeIf { it.isNotBlank() },
                imageUri = imageUri,
                mimeType = o.optString("mimeType").takeIf { it.isNotBlank() },
                category = runCatching { Category.valueOf(o.optString("category")) }.getOrDefault(Category.UNKNOWN),
                status = runCatching { ThingStatus.valueOf(o.optString("status")) }.getOrDefault(ThingStatus.ACTIVE),
                priority = runCatching { Priority.valueOf(o.optString("priority")) }.getOrDefault(Priority.NORMAL),
                dueAt = o.optLong("dueAt").takeIf { o.has("dueAt") && !o.isNull("dueAt") && it != 0L },
                isPinned = o.optBoolean("isPinned"),
                tags = o.optString("tags").split("|").filter { it.isNotBlank() },
                checklist = o.optString("checklist"),
                pinColor = o.optString("pinColor").ifBlank { "forest" },
                sortOrder = o.optInt("sortOrder"),
                expiresAt = o.optLong("expiresAt").takeIf { o.has("expiresAt") && !o.isNull("expiresAt") && it != 0L },
                ogImageUrl = o.optString("ogImageUrl").takeIf { it.isNotBlank() },
                siteName = o.optString("siteName").takeIf { it.isNotBlank() },
                ocrText = o.optString("ocrText").takeIf { it.isNotBlank() },
                notifId = dao.maxNotifId() + 1 + i,
            ).toEntity()
        }
        dao.upsertAll(entities)
    }

    private suspend fun patch(id: String, transform: suspend (Thing) -> Thing): Thing? = write.withLock {
        val existing = dao.getThing(id)?.toDomain() ?: return@withLock null
        val next = transform(existing).copy(updatedAt = System.currentTimeMillis())
        dao.upsert(next.toEntity())
        next
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
}
