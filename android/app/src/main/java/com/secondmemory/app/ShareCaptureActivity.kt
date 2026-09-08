package com.secondmemory.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.secondmemory.app.domain.CaptureInput
import com.secondmemory.app.domain.Heuristics
import com.secondmemory.app.notify.NotificationHelper
import com.secondmemory.app.notify.ShadeSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ShareCaptureActivity : ComponentActivity() {
    private var pendingMessage = "Saved"

    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        lifecycleScope.launch {
            (application as SecondMemoryApp).container.repository.patchSettings {
                it.copy(notificationsEnabled = granted, notificationsAsked = true)
            }
            publishAndFinish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }
        lifecycleScope.launch { runCapture() }
    }

    private suspend fun runCapture() {
        val app = application as SecondMemoryApp
        val inputs = withContext(Dispatchers.IO) { parseIntent(intent) }
        if (inputs.isEmpty()) {
            pendingMessage = "Nothing to save"
            publishAndFinish()
            return
        }
        var lastMessage = getString(R.string.saved_toast)
        for (input in inputs) {
            val result = app.container.repository.capture(input.copy(imageUri = null))
            var saved = result.saved
            if (result.blocked) {
                lastMessage = "Free limit reached — upgrade in Settings"
                continue
            }
            if (result.duplicate != null) {
                lastMessage = "Already saved: ${result.duplicate.title}"
                if (saved != null) NotificationHelper.showJustSaved(this, saved)
                continue
            }
            val pending = input.pendingStream
            if (saved != null && pending != null) {
                val path = persistSharedFile(Uri.parse(pending), input.mimeType, input.fileName)
                if (path == null) {
                    app.container.repository.remove(saved.id)
                    lastMessage = "Couldn’t save that file"
                    saved = null
                } else {
                    app.container.repository.setImageUri(saved.id, path)
                }
            }
            if (saved != null) {
                withContext(Dispatchers.IO) { app.container.repository.enrich(saved.id) }
                lastMessage = getString(R.string.saved_toast)
                NotificationHelper.showJustSaved(this, saved)
            }
        }
        pendingMessage = lastMessage
        val needPerm = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needPerm) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            publishAndFinish()
        }
    }

    private suspend fun publishAndFinish() {
        val app = application as SecondMemoryApp
        withContext(Dispatchers.IO) {
            ShadeSync.refresh(this@ShareCaptureActivity, app.container.repository)
        }
        Toast.makeText(this, pendingMessage, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun parseIntent(intent: Intent): List<CaptureInput> {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val html = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
        val combined = listOfNotNull(subject, text, html).distinct().joinToString("\n").ifBlank { null }
        val mime = intent.type
        val source = friendlySource()
        val streams = streamUris(intent)
        if (streams.isEmpty()) {
            if (combined.isNullOrBlank()) return emptyList()
            return listOf(
                CaptureInput(
                    text = combined,
                    url = Heuristics.extractFirstUrl(combined),
                    mimeType = mime,
                    sourceApp = source,
                ),
            )
        }
        return streams.map { uri ->
            val name = queryName(uri)
            val resolvedMime = contentResolver.getType(uri) ?: mime
            CaptureInput(
                text = combined,
                url = Heuristics.extractFirstUrl(combined.orEmpty()),
                mimeType = resolvedMime,
                sourceApp = source,
                fileName = name,
                pendingStream = uri.toString(),
            )
        }
    }

    private fun streamUris(intent: Intent): List<Uri> {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val list = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            return list.orEmpty().filterNotNull()
        }
        val one = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        return listOfNotNull(one)
    }

    private fun friendlySource(): String {
        @Suppress("DEPRECATION")
        val extra = intent.getParcelableExtra(Intent.EXTRA_REFERRER) as? Uri
        val fromReferrer = referrer?.host ?: extra?.host
        if (!fromReferrer.isNullOrBlank()) return fromReferrer
        val pkg = callingPackage ?: return "Share"
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            "Share"
        }
    }

    private fun queryName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    private fun persistSharedFile(uri: Uri, mime: String?, name: String?): String? {
        return try {
            val size = querySize(uri)
            if (size != null && size > com.secondmemory.app.data.CaptureFiles.MAX_BYTES) return null
            val dir = com.secondmemory.app.data.CaptureFiles.dir(this)
            val ext = name?.substringAfterLast('.', "")?.takeIf { it.length in 1..8 }
                ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                ?: when {
                    mime?.startsWith("image/") == true -> "jpg"
                    mime?.startsWith("video/") == true -> "mp4"
                    mime?.startsWith("audio/") == true -> "m4a"
                    mime?.contains("pdf") == true -> "pdf"
                    else -> "bin"
                }
            val dest = File(dir, "${UUID.randomUUID()}.$ext")
            var copied = 0L
            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        copied += n
                        if (copied > com.secondmemory.app.data.CaptureFiles.MAX_BYTES) {
                            dest.delete()
                            return null
                        }
                        output.write(buf, 0, n)
                    }
                }
            } ?: return null
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun querySize(uri: Uri): Long? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0).takeIf { it > 0 } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
