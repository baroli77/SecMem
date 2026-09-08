package com.secondmemory.app

import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.domain.Category
import com.secondmemory.app.domain.Heuristics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HeuristicsTest {
    @Test
    fun youtubeIsWatch() {
        val parsed = Heuristics.parseCaptureInput(
            CaptureInput(text = "https://www.youtube.com/watch?v=jNQXAC9IVRw"),
        )
        assertEquals(Category.WATCH, parsed.category)
        assertTrue(parsed.sourceUrl!!.contains("youtube.com"))
    }

    @Test
    fun amazonIsBuy() {
        val parsed = Heuristics.parseCaptureInput(
            CaptureInput(text = "https://www.amazon.com/dp/B09XS7J49B"),
        )
        assertEquals(Category.BUY, parsed.category)
    }

    @Test
    fun sarahSpreadsheetIsDo() {
        val parsed = Heuristics.parseCaptureInput(
            CaptureInput(text = "Don't forget to send Sarah the spreadsheet tomorrow."),
        )
        assertEquals(Category.DO, parsed.category)
        assertEquals("Sarah", parsed.detectedPerson)
        assertNotNull(parsed.detectedDate)
        assertNotNull(parsed.dueAt)
    }

    @Test
    fun dentistIsEvent() {
        val parsed = Heuristics.parseCaptureInput(
            CaptureInput(text = "Dentist appointment 14 October 10:40"),
        )
        assertEquals(Category.EVENT, parsed.category)
        assertEquals("10:40", parsed.detectedTime)
    }

    @Test
    fun tomorrowResolves() {
        val tz = TimeZone.getTimeZone("UTC")
        val now = Calendar.getInstance(tz).apply {
            set(2026, Calendar.SEPTEMBER, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("call them tomorrow", now.timeInMillis)
        assertEquals("2026-09-07", r.isoDate)
    }

    @Test
    fun thisWeekendOnSaturdayStaysToday() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 5, 10, 0, 0) // Saturday
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("do this weekend", now.timeInMillis)
        assertEquals("2026-09-05", r.isoDate)
    }

    @Test
    fun pastTimeRollsForward() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 8, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("call at 9am", now.timeInMillis)
        assertNotNull(r.dueAt)
        assertTrue(r.dueAt!! > now.timeInMillis)
    }

    @Test
    fun explicitDateResolves() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("Dentist 14 October 10:40", now.timeInMillis)
        assertEquals("2026-10-14", r.isoDate)
        assertEquals("10:40", r.time)
    }

    @Test
    fun dentistKeepsNineAm() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 8, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("Dentist 14 October at 9am", now.timeInMillis)
        assertEquals("09:00", r.time)
    }

    @Test
    fun ukNumericDate() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("Meet 14/10/2026", now.timeInMillis)
        assertEquals("2026-10-14", r.isoDate)
    }

    @Test
    fun restoredDuplicateClearsStale() {
        val stale = com.secondmemory.app.domain.Thing(
            id = "x",
            createdAt = 1,
            updatedAt = 1,
            originalContent = "https://example.com",
            contentType = com.secondmemory.app.domain.ContentType.URL,
            sourceUrl = "https://example.com",
            title = "Example",
            status = com.secondmemory.app.domain.ThingStatus.COMPLETED,
            completedAt = 99,
            resurfaceAt = 50,
            reasonForResurface = "old",
            isPinned = false,
        )
        val next = com.secondmemory.app.domain.PinStyle.restoredDuplicate(stale)
        assertEquals(com.secondmemory.app.domain.ThingStatus.ACTIVE, next.status)
        assertEquals(true, next.isPinned)
        org.junit.Assert.assertNull(next.completedAt)
        org.junit.Assert.assertNull(next.resurfaceAt)
    }
}
