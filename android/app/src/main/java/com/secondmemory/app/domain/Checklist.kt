package com.secondmemory.app.domain

data class CheckItem(val text: String, val done: Boolean)

object Checklist {
    fun parse(raw: String): List<CheckItem> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val tab = trimmed.indexOf('\t')
            if (tab > 0 && (trimmed.endsWith("\t1") || trimmed.endsWith("\t0") || trimmed[tab + 1] == '0' || trimmed[tab + 1] == '1')) {
                val text = trimmed.substring(0, tab).trim()
                val done = trimmed.substring(tab + 1).trim() == "1"
                if (text.isEmpty()) null else CheckItem(text, done)
            } else {
                val box = Regex("""^\s*\[([ xX])\]\s*(.+)$""").find(trimmed)
                if (box != null) CheckItem(box.groupValues[2].trim(), box.groupValues[1].isNotBlank() && box.groupValues[1] != " ")
                else CheckItem(trimmed, false)
            }
        }.toList()
    }

    fun format(items: List<CheckItem>): String =
        items.joinToString("\n") { "${it.text}\t${if (it.done) 1 else 0}" }

    fun fromNotes(notes: String?): List<CheckItem> {
        if (notes.isNullOrBlank()) return emptyList()
        return notes.lineSequence().mapNotNull { line ->
            val box = Regex("""^\s*\[([ xX])\]\s*(.+)$""").find(line) ?: return@mapNotNull null
            CheckItem(box.groupValues[2].trim(), box.groupValues[1].equals("x", true))
        }.toList()
    }

    fun toggle(items: List<CheckItem>, index: Int): List<CheckItem> =
        items.mapIndexed { i, item -> if (i == index) item.copy(done = !item.done) else item }

    fun checkNext(items: List<CheckItem>): List<CheckItem> {
        val i = items.indexOfFirst { !it.done }
        return if (i < 0) items else toggle(items, i)
    }

    fun linesForNotification(items: List<CheckItem>, limit: Int = 6): List<String> =
        items.take(limit).map { "${if (it.done) "☑" else "☐"}  ${it.text}" }
}
