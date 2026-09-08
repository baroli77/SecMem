package com.secondmemory.app.data

import android.content.Context
import java.io.File

object CaptureFiles {
    const val MAX_BYTES = 25L * 1024 * 1024

    fun dir(context: Context): File = File(context.filesDir, "captures").apply { mkdirs() }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.exists()) runCatching { file.delete() }
    }

    fun deleteAll(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    fun pruneOrphans(dir: File, keptPaths: Set<String>) {
        if (!dir.exists()) return
        val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
        dir.listFiles()?.forEach { file ->
            val path = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            if (path.startsWith(canonical) && file.absolutePath !in keptPaths && path !in keptPaths) {
                runCatching { file.delete() }
            }
        }
    }
}
