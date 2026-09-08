package com.secondmemory.app.domain

enum class Category {
    READ, WATCH, BUY, DO, WORK, PERSONAL, PLACE, IDEA, REFERENCE, EVENT, RECIPE, UNKNOWN
}

enum class ThingStatus { INBOX, ACTIVE, COMPLETED, ARCHIVED }

enum class ProcessingStatus { NONE, QUEUED, PROCESSING, COMPLETE, FAILED }

enum class ContentType { TEXT, URL, IMAGE, HTML, PDF, VIDEO, AUDIO, FILE, CONTACT, LOCATION }

enum class Priority { LOW, NORMAL, HIGH }

enum class Appearance { SYSTEM, LIGHT, DARK }

data class Thing(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val originalContent: String,
    val contentType: ContentType,
    val sourceUrl: String? = null,
    val sourceApp: String? = null,
    val title: String,
    val summary: String? = null,
    val notes: String? = null,
    val imageUri: String? = null,
    val mimeType: String? = null,
    val category: Category = Category.UNKNOWN,
    val status: ThingStatus = ThingStatus.INBOX,
    val priority: Priority = Priority.NORMAL,
    val dueAt: Long? = null,
    val resurfaceAt: Long? = null,
    val completedAt: Long? = null,
    val archivedAt: Long? = null,
    val lastOpenedAt: Long? = null,
    val lastResurfacedAt: Long? = null,
    val resurfaceCount: Int = 0,
    val isPinned: Boolean = false,
    val isFavourite: Boolean = false,
    val aiProcessed: Boolean = false,
    val aiConfidence: Float? = null,
    val aiProvider: String? = null,
    val processingStatus: ProcessingStatus = ProcessingStatus.NONE,
    val processingError: String? = null,
    val tags: List<String> = emptyList(),
    val detectedAction: String? = null,
    val detectedDate: String? = null,
    val detectedTime: String? = null,
    val detectedLocation: String? = null,
    val detectedPerson: String? = null,
    val estimatedReadMinutes: Int? = null,
    val suggestedNotificationText: String? = null,
    val reasonForResurface: String? = null,
    val ocrText: String? = null,
    val siteName: String? = null,
)

data class ActivityEvent(
    val id: String,
    val at: Long,
    val type: String,
    val thingId: String? = null,
    val title: String? = null,
    val detail: String? = null,
)

data class Settings(
    val appearance: Appearance = Appearance.SYSTEM,
    val aiEnabled: Boolean = true,
    val automaticProcessing: Boolean = true,
    val resurfaceEnabled: Boolean = false,
    val maxNudgesPerDay: Int = 5,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val workHoursStart: String = "09:00",
    val workHoursEnd: String = "18:00",
    val readingTime: String = "19:30",
    val leisureTime: String = "20:30",
    val payday: Int = 28,
    val notificationsEnabled: Boolean = false,
    val notificationsAsked: Boolean = false,
    val onboardingComplete: Boolean = false,
    val isPro: Boolean = false,
    val nudgesOn: String = "",
    val nudgesToday: Int = 0,
)

data class CaptureInput(
    val text: String? = null,
    val url: String? = null,
    val imageUri: String? = null,
    val mimeType: String? = null,
    val sourceApp: String? = null,
    val fileName: String? = null,
    val forceDuplicate: Boolean = false,
)

data class CaptureResult(
    val thing: Thing,
    val duplicate: Thing? = null,
    val blocked: Boolean = false,
)

data class ParsedCapture(
    val originalContent: String,
    val contentType: ContentType,
    val sourceUrl: String? = null,
    val title: String,
    val summary: String? = null,
    val category: Category,
    val tags: List<String>,
    val detectedDate: String? = null,
    val detectedTime: String? = null,
    val detectedPerson: String? = null,
    val dueAt: Long? = null,
    val imageUri: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
    val sourceApp: String? = null,
)

data class UrlMetadata(
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val siteName: String? = null,
    val canonicalUrl: String? = null,
)

const val FREE_ACTIVE_LIMIT = 40

fun activeCount(things: List<Thing>): Int =
    things.count { it.status == ThingStatus.INBOX || it.status == ThingStatus.ACTIVE }

fun categoryLabel(category: Category): String = when (category) {
    Category.READ -> "Read"
    Category.WATCH -> "Watch"
    Category.BUY -> "Buy"
    Category.DO -> "Do"
    Category.WORK -> "Work"
    Category.PERSONAL -> "Personal"
    Category.PLACE -> "Place"
    Category.IDEA -> "Idea"
    Category.REFERENCE -> "Reference"
    Category.EVENT -> "Event"
    Category.RECIPE -> "Recipe"
    Category.UNKNOWN -> "Saved"
}

fun thingActionVerb(thing: Thing): String = when (thing.category) {
    Category.READ -> "Read"
    Category.WATCH -> "Watched"
    Category.BUY -> "Bought"
    Category.DO, Category.WORK -> "Done"
    Category.RECIPE -> "Cooked"
    Category.EVENT -> "Done"
    else -> "Done"
}
