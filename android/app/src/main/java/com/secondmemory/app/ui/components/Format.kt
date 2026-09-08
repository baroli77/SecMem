package com.secondmemory.app.ui.components

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun hostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()
}

fun relativeAge(at: Long, now: Long = System.currentTimeMillis()): String {
    val delta = (now - at).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(at))
    }
}

fun compactFuture(at: Long, now: Long = System.currentTimeMillis()): String {
    val delta = at - now
    if (delta <= 0) return "soon"
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return when {
        hours < 1 -> "in ${TimeUnit.MILLISECONDS.toMinutes(delta)}m"
        hours < 24 -> "in ${hours}h"
        else -> "in ${days}d"
    }
}
