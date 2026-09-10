package com.secondmemory.app.domain

import java.net.URI
import java.util.Calendar
import java.util.Locale

object Heuristics {
    private val URL_RE = Regex("""https?://[^\s<>"')\]]+""", RegexOption.IGNORE_CASE)
    private val GEO_RE = Regex("""geo:[^\s<>"')\]]+""", RegexOption.IGNORE_CASE)
    private val YOUTUBE_RE = Regex("""(?:youtube\.com|youtu\.be)""", RegexOption.IGNORE_CASE)
    private val AMAZON_RE = Regex("""(?:amazon\.|amzn\.|amznto\.)""", RegexOption.IGNORE_CASE)
    private val REDDIT_RE = Regex("""(?:reddit\.com|redd\.it)""", RegexOption.IGNORE_CASE)
    private val RECIPE_HOST_RE = Regex(
        """(?:allrecipes|seriouseats|nytcooking|bbcgoodfood|simplyrecipes|bonappetit|epicurious)""",
        RegexOption.IGNORE_CASE,
    )
    private val NEWS_RE = Regex(
        """(?:nytimes|bbc\.|theguardian|washingtonpost|theverge|arstechnica|wired\.|medium\.|substack|waitbutwhy|newyorker|economist|dailymail|mailonline|mol\.im|dailym\.ai|independent\.|telegraph\.|sky\.com/news|cnn\.|reuters|apnews|npr\.org|ft\.com|bloomberg|wsj\.|news\.|bbc\.co\.uk|thetimes|metro\.co|mirror\.co|standard\.co)""",
        RegexOption.IGNORE_CASE,
    )
    private val MAPS_RE = Regex("""(?:maps\.google|google\.com/maps|openstreetmap|what3words)""", RegexOption.IGNORE_CASE)
    private val SHOP_RE = Regex("""(?:ebay\.|etsy\.|ikea\.|apple\.com/.*shop|store\.)""", RegexOption.IGNORE_CASE)
    private val TASK_RE = Regex(
        """\b(don't forget|do not forget|remind me|remember to|need to|have to|must |todo|to-do|follow up|send .+ the|call |email |pay |book |schedule )\b""",
        RegexOption.IGNORE_CASE,
    )
    private val RECIPE_TEXT_RE = Regex("""\b(ingredients|preheat|tbsp|tablespoon|bake for|serves \d|recipe)\b""", RegexOption.IGNORE_CASE)
    private val EVENT_RE = Regex(
        """\b(appointment|meeting|dentist|doctor|flight|reservation|booking|interview|birthday|wedding)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val WATCH_TEXT_RE = Regex("""\b(watch this|youtube|video essay|trailer)\b""", RegexOption.IGNORE_CASE)
    private val BUY_TEXT_RE = Regex("""\b(buy|purchase|wishlist|add to cart|on sale)\b""", RegexOption.IGNORE_CASE)
    private val IDEA_RE = Regex("""\b(idea:|what if|maybe we should|note to self)\b""", RegexOption.IGNORE_CASE)
    private val WEEKDAYS = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
    private val MONTHS = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
    )

    fun extractFirstUrl(text: String): String? {
        GEO_RE.find(text)?.value?.let { return it.trim().trimEnd('.', ',', ';') }
        val match = URL_RE.find(text) ?: return null
        return match.value.replace(Regex("""[.,;:]+$"""), "")
    }

    fun parseCaptureInput(input: CaptureInput, now: Long = System.currentTimeMillis()): ParsedCapture {
        val rawText = input.text?.trim().orEmpty()
        val explicitUrl = input.url?.trim()?.takeIf { it.isNotEmpty() } ?: extractFirstUrl(rawText)
        val mime = input.mimeType.orEmpty().lowercase(Locale.ROOT)
        val hasFile = !input.imageUri.isNullOrBlank() || !input.pendingStream.isNullOrBlank()
        val isVcard = mime.contains("vcard") || rawText.contains("BEGIN:VCARD", ignoreCase = true)
        val isGeo = rawText.startsWith("geo:", ignoreCase = true) ||
            mime.contains("vnd.android.cursor.item/place") ||
            Regex("""maps\.google|goo\.gl/maps|maps\.app\.goo""", RegexOption.IGNORE_CASE).containsMatchIn(rawText)

        val contentType = when {
            isVcard -> ContentType.CONTACT
            isGeo -> ContentType.LOCATION
            mime.startsWith("image/") -> ContentType.IMAGE
            mime.startsWith("video/") -> ContentType.VIDEO
            mime.startsWith("audio/") -> ContentType.AUDIO
            mime.contains("pdf") -> ContentType.PDF
            hasFile && mime.startsWith("image") -> ContentType.IMAGE
            hasFile && explicitUrl == null -> ContentType.FILE
            explicitUrl != null -> ContentType.URL
            Regex("""</?[a-z][\s\S]*>""", RegexOption.IGNORE_CASE).containsMatchIn(rawText) -> ContentType.HTML
            else -> ContentType.TEXT
        }

        val originalContent = when {
            rawText.isNotEmpty() -> rawText
            explicitUrl != null -> explicitUrl
            !input.fileName.isNullOrBlank() -> input.fileName
            contentType == ContentType.IMAGE -> photoTitle(now)
            else -> input.fileName ?: kindTitle(contentType, now)
        }

        val extracted = extractSignals(originalContent, explicitUrl, now)
        val fileTitle = prettyFileName(input.fileName)
        val title = when (contentType) {
            ContentType.CONTACT -> extractVcardName(rawText) ?: "Contact"
            ContentType.LOCATION -> extractLocationLabel(rawText) ?: "Dropped pin"
            ContentType.IMAGE -> if (rawText.length >= 3 && !rawText.startsWith("http")) inferTitle(originalContent, explicitUrl, true, now) else photoTitle(now)
            ContentType.VIDEO, ContentType.AUDIO, ContentType.PDF, ContentType.FILE ->
                fileTitle ?: kindTitle(contentType, now)
            else -> inferTitle(originalContent, explicitUrl, false, now)
        }

        val category = when (contentType) {
            ContentType.VIDEO, ContentType.AUDIO -> Category.WATCH
            ContentType.PDF -> Category.READ
            ContentType.CONTACT -> Category.PERSONAL
            ContentType.LOCATION -> Category.PLACE
            ContentType.FILE -> Category.REFERENCE
            ContentType.IMAGE -> if (extracted.category != Category.UNKNOWN) extracted.category else Category.PERSONAL
            else -> extracted.category
        }

        return ParsedCapture(
            originalContent = originalContent,
            contentType = contentType,
            sourceUrl = explicitUrl,
            title = title,
            summary = extracted.summary,
            category = category,
            tags = extracted.tags,
            detectedDate = extracted.detectedDate,
            detectedTime = extracted.detectedTime,
            detectedPerson = extracted.detectedPerson ?: extractVcardName(rawText),
            dueAt = extracted.dueAt,
            imageUri = input.imageUri,
            mimeType = input.mimeType,
            fileName = input.fileName,
            sourceApp = input.sourceApp,
        )
    }

    fun prettyFileName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val base = name.substringAfterLast('/').substringBeforeLast('.')
            .replace(Regex("""[-_]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (base.length < 2 || Regex("""^[0-9a-f-]{8,}$""", RegexOption.IGNORE_CASE).matches(base)) return null
        return truncate(titleCase(base), 90)
    }

    fun kindTitle(type: ContentType, now: Long): String {
        val fmt = java.text.SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
        val whenStr = fmt.format(java.util.Date(now))
        return when (type) {
            ContentType.IMAGE -> "Photo · $whenStr"
            ContentType.VIDEO -> "Video · $whenStr"
            ContentType.AUDIO -> "Audio · $whenStr"
            ContentType.PDF -> "PDF · $whenStr"
            ContentType.FILE -> "File · $whenStr"
            ContentType.CONTACT -> "Contact"
            ContentType.LOCATION -> "Dropped pin"
            else -> "Saved · $whenStr"
        }
    }

    fun extractVcardName(text: String): String? {
        val fn = Regex("""^FN[;:](.+)$""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)).find(text)
        return fn?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun extractLocationLabel(text: String): String? {
        if (text.startsWith("geo:", ignoreCase = true)) {
            val coords = text.removePrefix("geo:").substringBefore('?').substringBefore(';')
            return "Pin · $coords"
        }
        return inferTitle(text, extractFirstUrl(text))
    }

    fun inferTitle(content: String, url: String?, isImage: Boolean = false, now: Long = System.currentTimeMillis()): String {
        val firstLine = content.split(Regex("""\n+"""))
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && it != url && !it.startsWith("http") }

        if (!firstLine.isNullOrEmpty() && firstLine != url && !firstLine.startsWith("Photo ·")) {
            val cleaned = firstLine.replace(URL_RE, "").trim()
            if (cleaned.length >= 3) return truncate(cleaned, 90)
        }

        if (url != null) {
            try {
                val u = URI(url)
                val host = (u.host ?: "").replace(Regex("""^www\."""), "")
                val last = u.path.orEmpty()
                    .split("/")
                    .filter { it.isNotEmpty() }
                    .lastOrNull()
                    ?.replace(Regex("""[-_]"""), " ")
                    ?.replace(Regex("""\.\w+$"""), "")
                if (!last.isNullOrEmpty() && last.length > 2 && !Regex("""^[a-z0-9]{8,}$""", RegexOption.IGNORE_CASE).matches(last)) {
                    return truncate(titleCase(last), 90)
                }
                if (host.isNotEmpty()) return host
            } catch (_: Exception) {
                return truncate(url, 90)
            }
        }

        if (isImage) return photoTitle(now)
        return truncate(content, 90).ifEmpty { "Untitled" }
    }

    fun photoTitle(now: Long = System.currentTimeMillis()): String {
        val fmt = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault())
        return "Photo · ${fmt.format(java.util.Date(now))}"
    }

    data class ExtractedSignals(
        val category: Category,
        val tags: List<String>,
        val summary: String? = null,
        val detectedDate: String? = null,
        val detectedTime: String? = null,
        val detectedPerson: String? = null,
        val dueAt: Long? = null,
    )

    fun extractSignals(content: String, url: String?, now: Long = System.currentTimeMillis()): ExtractedSignals {
        val text = content.lowercase(Locale.ROOT)
        var category = Category.UNKNOWN
        val tags = mutableListOf<String>()

        when {
            url != null && YOUTUBE_RE.containsMatchIn(url) -> {
                category = Category.WATCH
                tags += "video"
            }
            url != null && (AMAZON_RE.containsMatchIn(url) || SHOP_RE.containsMatchIn(url)) -> {
                category = Category.BUY
                tags += "product"
            }
            url != null && REDDIT_RE.containsMatchIn(url) -> {
                category = Category.READ
                tags += "reddit"
            }
            url != null && RECIPE_HOST_RE.containsMatchIn(url) -> {
                category = Category.RECIPE
                tags += "cooking"
            }
            url != null && MAPS_RE.containsMatchIn(url) -> category = Category.PLACE
            url != null && NEWS_RE.containsMatchIn(url) -> {
                category = Category.READ
                tags += "article"
            }
            url == null && RECIPE_TEXT_RE.containsMatchIn(text) -> category = Category.RECIPE
            url == null && EVENT_RE.containsMatchIn(text) && hasDateCue(text) -> category = Category.EVENT
            url == null && TASK_RE.containsMatchIn(content) -> category = Category.DO
            url == null && WATCH_TEXT_RE.containsMatchIn(text) -> category = Category.WATCH
            url == null && BUY_TEXT_RE.containsMatchIn(text) -> category = Category.BUY
            url == null && IDEA_RE.containsMatchIn(text) -> category = Category.IDEA
            url != null -> {
                category = Category.READ
                tags += "article"
            }
            EVENT_RE.containsMatchIn(text) -> category = Category.EVENT
        }

        if (Regex("""\b(work|slack|jira|standup|office)\b""", RegexOption.IGNORE_CASE).containsMatchIn(content) &&
            category == Category.DO
        ) {
            category = Category.WORK
        }

        val whenDate = extractDateTime(content, now)
        val person = extractPerson(content)
        if (whenDate.isoDate != null) tags += "dated"
        if (person != null) tags += "person"

        val firstSentence = content
            .replace(URL_RE, "")
            .split(Regex("""(?<=[.!?])\s+"""))
            .map { it.trim() }
            .firstOrNull { it.length > 20 }

        return ExtractedSignals(
            category = category,
            tags = tags.distinct().take(6),
            summary = firstSentence?.let { truncate(it, 180) },
            detectedDate = whenDate.isoDate,
            detectedTime = whenDate.time,
            detectedPerson = person,
            dueAt = whenDate.dueAt,
        )
    }

    data class DateTimeHit(val isoDate: String? = null, val time: String? = null, val dueAt: Long? = null)

    fun extractDateTime(content: String, now: Long = System.currentTimeMillis()): DateTimeHit {
        val text = content.lowercase(Locale.ROOT)
        val nowCal = calendarOf(now)
        var date = nowCal.clone() as Calendar
        var foundDate = false
        var time: String? = null
        var hour: Int? = null
        var minute = 0

        val explicitTime = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(content)
        val merTime = Regex("""\b(\d{1,2})(?::([0-5]\d))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE).find(content)
        if (merTime != null) {
            hour = merTime.groupValues[1].toInt()
            minute = merTime.groupValues[2].ifEmpty { "0" }.toInt()
            val mer = merTime.groupValues[3].lowercase(Locale.ROOT)
            var h = hour!!
            if (mer == "pm" && h < 12) h += 12
            if (mer == "am" && h == 12) h = 0
            hour = h
            time = "${pad(h)}:${pad(minute)}"
        } else if (explicitTime != null) {
            hour = explicitTime.groupValues[1].toInt()
            minute = explicitTime.groupValues[2].toInt()
            time = "${pad(hour)}:${pad(minute)}"
        }

        when {
            Regex("""\btoday\b""").containsMatchIn(text) || Regex("""\btonight\b""").containsMatchIn(text) -> {
                foundDate = true
                date = nowCal.clone() as Calendar
            }
            Regex("""\btomorrow\b""").containsMatchIn(text) -> {
                foundDate = true
                date = addDays(nowCal, 1)
            }
            Regex("""\bthis weekend\b""").containsMatchIn(text) -> {
                foundDate = true
                date = nextWeekend(nowCal)
            }
            Regex("""\bnext week\b""").containsMatchIn(text) -> {
                foundDate = true
                date = addDays(nowCal, 7)
            }
            else -> {
                for (i in WEEKDAYS.indices) {
                    val name = WEEKDAYS[i]
                    if (Regex("""\b(next\s+)?$name\b""").containsMatchIn(text)) {
                        foundDate = true
                        date = nextWeekday(nowCal, i, allowToday = false)
                        break
                    }
                }
            }
        }

        Regex("""\b(20\d{2}-\d{2}-\d{2})\b""").find(content)?.let {
            val parts = it.groupValues[1].split("-")
            parsedCalendar(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())?.let { cal ->
                foundDate = true
                date = cal
            }
        }

        Regex("""\b(\d{1,2})/(\d{1,2})/(20\d{2}|\d{2})\b""").find(content)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            var year = m.groupValues[3].toInt()
            if (year < 100) year += 2000
            if (day in 1..31 && month in 1..12) {
                parsedCalendar(year, month - 1, day)?.let { cal ->
                    foundDate = true
                    date = cal
                }
            }
        }

        val dayMonth = Regex(
            """\b(\d{1,2})(?:st|nd|rd|th)?\s+(january|february|march|april|may|june|july|august|september|october|november|december)\b""",
            RegexOption.IGNORE_CASE,
        ).find(content)
        val monthDay = Regex(
            """\b(january|february|march|april|may|june|july|august|september|october|november|december)\s+(\d{1,2})(?:st|nd|rd|th)?\b""",
            RegexOption.IGNORE_CASE,
        ).find(content)
        if (dayMonth != null || monthDay != null) {
            val monthName = (dayMonth?.groupValues?.get(2) ?: monthDay?.groupValues?.get(1) ?: "").lowercase(Locale.ROOT)
            val dayNum = (dayMonth?.groupValues?.get(1) ?: monthDay?.groupValues?.get(2))?.toIntOrNull() ?: 0
            val mi = MONTHS.indexOf(monthName)
            if (mi >= 0 && dayNum in 1..31) {
                foundDate = true
                date = Calendar.getInstance().apply {
                    set(nowCal.get(Calendar.YEAR), mi, dayNum, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (date.timeInMillis < now - 12 * 3600_000L) {
                    date.add(Calendar.YEAR, 1)
                }
            }
        }

        if (!foundDate && time == null) return DateTimeHit()

        return try {
            when {
                hour != null -> {
                    date.set(Calendar.HOUR_OF_DAY, hour)
                    date.set(Calendar.MINUTE, minute)
                    date.set(Calendar.SECOND, 0)
                    date.set(Calendar.MILLISECOND, 0)
                }
                Regex("""\btonight\b""").containsMatchIn(text) -> {
                    date.set(Calendar.HOUR_OF_DAY, 20)
                    date.set(Calendar.MINUTE, 0)
                    date.set(Calendar.SECOND, 0)
                    date.set(Calendar.MILLISECOND, 0)
                }
                foundDate -> {
                    date.set(Calendar.HOUR_OF_DAY, 9)
                    date.set(Calendar.MINUTE, 0)
                    date.set(Calendar.SECOND, 0)
                    date.set(Calendar.MILLISECOND, 0)
                }
            }

            var dueAt = if (foundDate || time != null) date.timeInMillis else null
            if (dueAt != null && dueAt < now - 30_000L) {
                val todayish = Regex("""\b(today|tonight)\b""").containsMatchIn(text)
                if (!foundDate || todayish) {
                    date.add(Calendar.DAY_OF_YEAR, 1)
                    dueAt = date.timeInMillis
                }
            }
            DateTimeHit(
                isoDate = if (foundDate) "${date.get(Calendar.YEAR)}-${pad(date.get(Calendar.MONTH) + 1)}-${pad(date.get(Calendar.DAY_OF_MONTH))}" else null,
                time = time,
                dueAt = dueAt,
            )
        } catch (_: Exception) {
            DateTimeHit()
        }
    }

    private fun parsedCalendar(year: Int, monthZero: Int, day: Int): Calendar? {
        if (monthZero !in 0..11 || day !in 1..31) return null
        return try {
            Calendar.getInstance().apply {
                isLenient = false
                set(year, monthZero, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractPerson(content: String): String? {
        val match = Regex("""\b(?:send|tell|call|email|meet|ask|ping|text|message)\s+([A-Z][a-z]+)\b""").find(content)
        val name = match?.groupValues?.get(1)
        return name?.takeIf { it !in setOf("This", "The", "A", "An", "My") }
    }

    fun findDuplicate(things: List<Thing>, sourceUrl: String?): Thing? {
        if (sourceUrl.isNullOrBlank()) return null
        val normalized = normalizeUrl(sourceUrl)
        return things
            .filter { it.sourceUrl != null && normalizeUrl(it.sourceUrl) == normalized && it.status != ThingStatus.ARCHIVED }
            .maxByOrNull { it.createdAt }
    }

    fun normalizeUrl(url: String): String {
        return try {
            val u = URI(url)
            val host = (u.host ?: "").replace(Regex("""^www\."""), "").lowercase(Locale.ROOT)
            var path = u.path ?: ""
            if (path.endsWith("/") && path.length > 1) path = path.dropLast(1)
            val query = u.query
                ?.split("&")
                ?.filterNot { it.startsWith("utm_") }
                ?.joinToString("&")
                .orEmpty()
            val q = if (query.isEmpty()) "" else "?$query"
            "https://$host$path$q"
        } catch (_: Exception) {
            url.trim().lowercase(Locale.ROOT)
        }
    }

    private fun hasDateCue(text: String): Boolean =
        Regex("""\b(today|tomorrow|tonight|monday|tuesday|wednesday|thursday|friday|saturday|sunday|january|february|march|april|may|june|july|august|september|october|november|december|\d{1,2}:\d{2}|20\d{2}-\d{2}-\d{2})\b""")
            .containsMatchIn(text)

    private fun calendarOf(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    private fun addDays(from: Calendar, n: Int): Calendar =
        (from.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, n) }

    private fun nextWeekend(from: Calendar): Calendar {
        val dow = from.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return from.clone() as Calendar
        return nextWeekday(from, 6, allowToday = true)
    }

    private fun nextWeekday(from: Calendar, jsWeekday: Int, allowToday: Boolean = false): Calendar {
        val target = jsWeekday + 1
        val d = from.clone() as Calendar
        var diff = (target - d.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (diff == 0 && !allowToday) diff = 7
        d.add(Calendar.DAY_OF_YEAR, diff)
        return d
    }

    internal fun pad(n: Int): String = n.toString().padStart(2, '0')

    private fun titleCase(s: String): String =
        s.split(Regex("""\s+""")).joinToString(" ") { w ->
            if (w.isEmpty()) w else w[0].uppercase(Locale.ROOT) + w.substring(1).lowercase(Locale.ROOT)
        }

    fun truncate(s: String, n: Int): String {
        val t = s.replace(Regex("""\s+"""), " ").trim()
        return if (t.length <= n) t else t.take(n - 1).trimEnd() + "…"
    }
}
