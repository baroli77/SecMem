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
        val now = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.SEPTEMBER, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val r = Heuristics.extractDateTime("call them tomorrow", now.timeInMillis)
        assertEquals("2026-09-07", r.isoDate)
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
    fun normalizeStripsUtm() {
        val a = Heuristics.normalizeUrl("https://www.Example.com/path/?utm_source=x")
        val b = Heuristics.normalizeUrl("https://example.com/path")
        assertEquals(a, b)
    }
}
