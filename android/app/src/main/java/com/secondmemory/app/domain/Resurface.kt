package com.secondmemory.app.domain

import java.util.Calendar

data class SnoozeOption(val id: String, val label: String, val at: Long)

data class TodayBuckets(
    val due: List<Thing>,
    val resurfaced: List<Thing>,
    val laterToday: List<Thing>,
    val suggested: List<Thing>,
    val stillWant: List<Thing>,
)

object Resurface {
    fun parseHm(hm: String): Pair<Int, Int> {
        val parts = hm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    fun atTime(base: Calendar, hm: String): Calendar {
        val (h, m) = parseHm(hm)
        return (base.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun atTime(millis: Long, hm: String): Calendar =
        atTime(Calendar.getInstance().apply { timeInMillis = millis }, hm)

    private fun toMinutes(hm: String): Int {
        val (h, m) = parseHm(hm)
        return h * 60 + m
    }

    fun isQuietHours(now: Calendar, settings: Settings): Boolean {
        val cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = toMinutes(settings.quietHoursStart)
        val end = toMinutes(settings.quietHoursEnd)
        if (start == end) return false
        return if (start < end) cur >= start && cur < end else cur >= start || cur < end
    }

    fun isWorkHours(now: Calendar, settings: Settings): Boolean {
        val day = now.get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.SUNDAY || day == Calendar.SATURDAY) return false
        val cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return cur >= toMinutes(settings.workHoursStart) && cur < toMinutes(settings.workHoursEnd)
    }

    fun nextWeekdayMorning(from: Calendar, hm: String): Calendar {
        var d = atTime(from, hm)
        if (d.timeInMillis <= from.timeInMillis) d.add(Calendar.DAY_OF_YEAR, 1)
        while (d.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || d.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            d.add(Calendar.DAY_OF_YEAR, 1)
        }
        return d
    }

    fun nextPayday(from: Calendar, payday: Int, hm: String = "10:00"): Calendar {
        val day = payday.coerceIn(1, 28)
        var candidate = atTime(
            Calendar.getInstance().apply {
                set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), day)
            },
            hm,
        )
        if (candidate.timeInMillis <= from.timeInMillis) {
            candidate = atTime(
                Calendar.getInstance().apply {
                    set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), day)
                    add(Calendar.MONTH, 1)
                },
                hm,
            )
        }
        return candidate
    }

    fun nextOccurrence(from: Calendar, hm: String): Calendar {
        var d = atTime(from, hm)
        if (d.timeInMillis <= from.timeInMillis + 30 * 60_000L) d.add(Calendar.DAY_OF_YEAR, 1)
        return d
    }

    fun nextSaturday(from: Calendar): Calendar {
        val d = from.clone() as Calendar
        val diff = (Calendar.SATURDAY - d.get(Calendar.DAY_OF_WEEK) + 7) % 7
        d.add(Calendar.DAY_OF_YEAR, if (diff == 0) 7 else diff)
        return d
    }

    fun suggestResurfaceAt(thing: Thing, settings: Settings, now: Calendar = Calendar.getInstance()): Pair<Long, String> {
        val dueAt = thing.dueAt
        if (dueAt != null) {
            val due = Calendar.getInstance().apply { timeInMillis = dueAt }
            if (thing.category == Category.EVENT) {
                val ahead = dueAt - 16 * 3600_000L
                if (ahead > now.timeInMillis + 20 * 60_000L) {
                    return ahead to "Ahead of the event"
                }
                val soon = dueAt - 90 * 60_000L
                return maxOf(soon, now.timeInMillis + 15 * 60_000L) to "Shortly before the event"
            }
            if (due.timeInMillis > now.timeInMillis + 20 * 60_000L) {
                return dueAt to "On the detected date"
            }
        }

        return when (thing.category) {
            Category.READ -> nextOccurrence(now, settings.readingTime).timeInMillis to "Evening reading window"
            Category.WATCH -> {
                val leisure = nextOccurrence(now, settings.leisureTime)
                val dow = now.get(Calendar.DAY_OF_WEEK)
                if (dow == Calendar.FRIDAY || dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                    leisure.timeInMillis to "Leisure period"
                } else {
                    val sat = atTime(nextSaturday(now), settings.leisureTime)
                    val pick = if (leisure.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        leisure.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    ) leisure else sat
                    pick.timeInMillis to "Evening or weekend leisure"
                }
            }
            Category.BUY -> nextPayday(now, settings.payday).timeInMillis to "Around payday"
            Category.RECIPE -> {
                var sat = atTime(nextSaturday(now), "10:00")
                if (now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY && now.get(Calendar.HOUR_OF_DAY) < 12) {
                    sat = atTime(now, "10:00")
                }
                sat.timeInMillis to "Weekend meal planning"
            }
            Category.WORK -> nextWeekdayMorning(now, settings.workHoursStart).timeInMillis to "Next work morning"
            Category.DO -> nextWeekdayMorning(now, settings.workHoursStart).timeInMillis to "Next useful working window"
            Category.EVENT -> nextOccurrence(now, "18:00").timeInMillis to "Ahead of the day"
            Category.PLACE -> {
                val d = atTime(now, settings.leisureTime)
                d.add(Calendar.DAY_OF_YEAR, 2)
                d.timeInMillis to "When there is time to go"
            }
            Category.IDEA, Category.REFERENCE -> {
                val d = atTime(now, settings.readingTime)
                d.add(Calendar.DAY_OF_YEAR, 3)
                d.timeInMillis to "Give it a few days, then review"
            }
            Category.PERSONAL -> nextOccurrence(now, settings.leisureTime).timeInMillis to "Personal time"
            else -> {
                val days = if (thing.isPinned) 2 else 4
                val d = atTime(now, settings.readingTime)
                d.add(Calendar.DAY_OF_YEAR, days)
                d.timeInMillis to "A later reminder"
            }
        }
    }

    fun snoozeOptions(now: Calendar = Calendar.getInstance(), settings: Settings = Settings()): List<SnoozeOption> {
        val reading = settings.readingTime
        val work = settings.workHoursStart
        val inHour = now.timeInMillis + 60 * 60_000L
        var tonight = atTime(now, reading)
        if (tonight.timeInMillis <= now.timeInMillis + 20 * 60_000L) tonight.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = atTime(now, work).apply { add(Calendar.DAY_OF_YEAR, 1) }
        var weekend = atTime(nextSaturday(now), "10:00")
        if (now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY && now.get(Calendar.HOUR_OF_DAY) < 10) {
            weekend = atTime(now, "10:00")
        }
        return listOf(
            SnoozeOption("1h", "1 hour", inHour),
            SnoozeOption("tonight", "Tonight", tonight.timeInMillis),
            SnoozeOption("tomorrow", "Tomorrow", tomorrow.timeInMillis),
            SnoozeOption("weekend", "Weekend", weekend.timeInMillis),
        )
    }

    fun notificationCopy(thing: Thing): String {
        thing.suggestedNotificationText?.let { return it }
        val title = thing.title.ifBlank { "this" }
        return when (thing.category) {
            Category.READ -> "Worth reading tonight?"
            Category.WATCH -> "You saved this to watch."
            Category.BUY -> "Still want ${Heuristics.truncate(title, 42)}?"
            Category.DO, Category.WORK -> thing.detectedAction ?: title
            Category.EVENT -> title
            Category.RECIPE -> "Planning meals? You saved this recipe."
            Category.IDEA -> "That idea is still here."
            else -> "You saved this for later."
        }
    }

    fun staleUnopened(thing: Thing, now: Long = System.currentTimeMillis()): Boolean {
        if (thing.status == ThingStatus.COMPLETED || thing.status == ThingStatus.ARCHIVED) return false
        val origin = thing.lastOpenedAt ?: thing.createdAt
        return now - origin > 30L * 24 * 3600_000
    }

    fun shouldNudge(settings: Settings, now: Calendar = Calendar.getInstance()): Pair<Boolean, String?> {
        if (!settings.resurfaceEnabled) return false to "disabled"
        if (isQuietHours(now, settings)) return false to "quiet"
        val day = formatDay(now)
        val used = if (settings.nudgesOn == day) settings.nudgesToday else 0
        if (used >= settings.maxNudgesPerDay) return false to "throttle"
        return true to null
    }

    fun formatDay(d: Calendar): String =
        "${d.get(Calendar.YEAR)}-${Heuristics.pad(d.get(Calendar.MONTH) + 1)}-${Heuristics.pad(d.get(Calendar.DAY_OF_MONTH))}"

    fun todayBuckets(things: List<Thing>, now: Long = System.currentTimeMillis()): TodayBuckets {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val open = things.filter { it.status != ThingStatus.COMPLETED && it.status != ThingStatus.ARCHIVED }
        val due = open.filter { it.dueAt != null && it.dueAt!! in start..end }.sortedBy { it.dueAt }
        val dueIds = due.map { it.id }.toSet()
        val resurfaced = open.filter {
            it.id !in dueIds && it.resurfaceAt != null && it.resurfaceAt!! <= now
        }.sortedBy { it.resurfaceAt }
        val resurfacedIds = resurfaced.map { it.id }.toSet()
        val laterToday = open.filter {
            it.id !in dueIds && it.id !in resurfacedIds &&
                it.resurfaceAt != null && it.resurfaceAt!! > now && it.resurfaceAt!! <= end
        }.sortedBy { it.resurfaceAt }
        val laterIds = laterToday.map { it.id }.toSet()
        val stillWant = open.filter {
            it.id !in dueIds && it.id !in resurfacedIds && it.id !in laterIds && staleUnopened(it, now)
        }.sortedBy { it.createdAt }.take(4)
        val stillIds = stillWant.map { it.id }.toSet()
        val suggested = open.filter {
            it.id !in dueIds && it.id !in resurfacedIds && it.id !in laterIds && it.id !in stillIds &&
                it.isPinned
        }.sortedWith(compareByDescending<Thing> { it.isPinned }.thenByDescending { it.updatedAt }).take(4)

        return TodayBuckets(due, resurfaced, laterToday, suggested, stillWant)
    }

    fun tick(things: List<Thing>, now: Long = System.currentTimeMillis()): List<Thing> {
        return things.filter {
            it.status != ThingStatus.COMPLETED &&
                it.status != ThingStatus.ARCHIVED &&
                it.resurfaceAt != null &&
                it.resurfaceAt!! <= now &&
                (it.lastResurfacedAt == null || it.lastResurfacedAt!! < it.resurfaceAt!!)
        }
    }
}
