package com.secondmemory.app.domain

import java.util.Calendar
import java.util.UUID

object Seed {
    fun examples(now: Long = System.currentTimeMillis()): List<Thing> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        fun shift(hours: Int): Long = now + hours * 3600_000L
        fun days(n: Int): Long = now + n * 24 * 3600_000L
        val tomorrowMorning = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val payday = Resurface.nextPayday(cal, 28).timeInMillis
        val isoTomorrow = Resurface.formatDay(
            Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, 1)
            },
        )

        return listOf(
            Thing(
                id = nid(),
                createdAt = shift(-3),
                updatedAt = shift(-3),
                originalContent = "Don't forget to send Sarah the spreadsheet tomorrow.",
                contentType = ContentType.TEXT,
                sourceApp = "Messages",
                title = "Send Sarah the spreadsheet",
                summary = "Follow up with Sarah and send the spreadsheet tomorrow.",
                category = Category.DO,
                status = ThingStatus.ACTIVE,
                isPinned = true,
                priority = Priority.HIGH,
                dueAt = tomorrowMorning,
                resurfaceAt = shift(1),
                aiProcessed = true,
                aiConfidence = 0.92f,
                aiProvider = "heuristic",
                processingStatus = ProcessingStatus.COMPLETE,
                tags = listOf("person", "dated"),
                detectedAction = "Send Sarah the spreadsheet",
                detectedPerson = "Sarah",
                detectedDate = isoTomorrow,
                suggestedNotificationText = "Send Sarah the spreadsheet.",
                reasonForResurface = "On the detected date",
            ),
            Thing(
                id = nid(),
                createdAt = shift(-26),
                updatedAt = shift(-2),
                originalContent = "https://waitbutwhy.com/2013/10/why-procrastinators-procrastinate.html",
                contentType = ContentType.URL,
                sourceUrl = "https://waitbutwhy.com/2013/10/why-procrastinators-procrastinate.html",
                sourceApp = "Chrome",
                title = "Why Procrastinators Procrastinate",
                summary = "A long, vivid look at the Instant Gratification Monkey and why important work keeps losing to easy now.",
                category = Category.READ,
                status = ThingStatus.ACTIVE,
                isPinned = true,
                resurfaceAt = now - 40 * 60_000L,
                lastResurfacedAt = now - 40 * 60_000L,
                resurfaceCount = 1,
                isFavourite = true,
                aiProcessed = true,
                aiConfidence = 0.88f,
                processingStatus = ProcessingStatus.COMPLETE,
                tags = listOf("article", "deep-work"),
                estimatedReadMinutes = 18,
                suggestedNotificationText = "Worth reading tonight?",
                reasonForResurface = "Evening reading window",
                siteName = "Wait But Why",
            ),
            Thing(
                id = nid(),
                createdAt = days(-2),
                updatedAt = days(-2),
                originalContent = "https://www.youtube.com/watch?v=jNQXAC9IVRw",
                contentType = ContentType.URL,
                sourceUrl = "https://www.youtube.com/watch?v=jNQXAC9IVRw",
                sourceApp = "YouTube",
                title = "Me at the zoo",
                summary = "The first video uploaded to YouTube — a 19-second clip at the zoo.",
                category = Category.WATCH,
                status = ThingStatus.ACTIVE,
                isPinned = true,
                priority = Priority.LOW,
                resurfaceAt = now - 12 * 60_000L,
                lastResurfacedAt = now - 12 * 60_000L,
                resurfaceCount = 1,
                aiProcessed = true,
                aiConfidence = 0.8f,
                processingStatus = ProcessingStatus.COMPLETE,
                tags = listOf("video"),
                estimatedReadMinutes = 1,
                suggestedNotificationText = "You saved this to watch.",
                reasonForResurface = "Leisure period",
                siteName = "YouTube",
            ),
            Thing(
                id = nid(),
                createdAt = days(-6),
                updatedAt = days(-6),
                originalContent = "Sony WH-1000XM7 wireless noise-cancelling headphones\nhttps://www.amazon.com/dp/B0C33XXS56",
                contentType = ContentType.URL,
                sourceUrl = "https://www.amazon.com/dp/B0C33XXS56",
                sourceApp = "Amazon",
                title = "Sony WH-1000XM7 Headphones",
                summary = "Sony wireless noise-cancelling headphones. Consider on payday.",
                category = Category.BUY,
                status = ThingStatus.ACTIVE,
                isPinned = true,
                resurfaceAt = payday,
                aiProcessed = true,
                aiConfidence = 0.86f,
                processingStatus = ProcessingStatus.COMPLETE,
                tags = listOf("product"),
                detectedAction = "Consider purchase",
                reasonForResurface = "Around payday",
                siteName = "Amazon",
            ),
            Thing(
                id = nid(),
                createdAt = days(-1),
                updatedAt = days(-1),
                originalContent = "Dentist appointment 14 October 10:40",
                contentType = ContentType.TEXT,
                sourceApp = "Notes",
                title = "Dentist appointment",
                summary = "Appointment at 10:40 on 14 October.",
                category = Category.EVENT,
                status = ThingStatus.ACTIVE,
                isPinned = true,
                priority = Priority.HIGH,
                detectedDate = "${cal.get(Calendar.YEAR)}-10-14",
                detectedTime = "10:40",
                dueAt = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), Calendar.OCTOBER, 14, 10, 40, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis < now) add(Calendar.YEAR, 1)
                }.timeInMillis,
                resurfaceAt = now + 6 * 3600_000L,
                aiProcessed = true,
                processingStatus = ProcessingStatus.COMPLETE,
                tags = listOf("dated"),
                reasonForResurface = "Ahead of the event",
            ),
        )
    }

    fun nid(): String = UUID.randomUUID().toString()
}
