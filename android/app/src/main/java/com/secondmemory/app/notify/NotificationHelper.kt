package com.secondmemory.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.secondmemory.app.MainActivity
import com.secondmemory.app.R
import com.secondmemory.app.domain.ContentType
import com.secondmemory.app.domain.Checklist
import com.secondmemory.app.domain.PinStyle
import com.secondmemory.app.domain.Priority
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.domain.thingActionVerb
import java.io.File

object NotificationHelper {
    const val CHANNEL_ID = "pins"
    const val ACTION_DONE = "com.secondmemory.app.DONE"
    const val ACTION_LATER = "com.secondmemory.app.LATER"
    const val ACTION_UNPIN = "com.secondmemory.app.UNPIN"
    const val ACTION_OPEN = "com.secondmemory.app.OPEN"
    const val ACTION_PIN = "com.secondmemory.app.PIN"
    const val ACTION_CHECK = "com.secondmemory.app.CHECK"
    const val EXTRA_THING_ID = "thingId"
    private const val GROUP = "pinned"
    private const val SUMMARY_ID = 1
    private const val WELCOME_ID = 2
    private const val PIN_LIMIT = 20
    private const val REPOST_WINDOW_MS = 20_000L

    fun isPinned(thing: Thing): Boolean = thing.isPinned || thing.isFavourite

    fun pinId(thing: Thing): Int =
        if (thing.notifId > 0) thing.notifId else 10_000 + (thing.id.hashCode() and 0x7fffffff) % 80_000

    private fun requestCode(thing: Thing, offset: Int): Int = pinId(thing) * 10 + offset

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel("resurface")
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_pins_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.channel_pins_desc)
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            )
        }
    }

    fun refreshPins(
        context: Context,
        things: List<Thing>,
        settings: Settings = Settings(),
        restoreMissing: Boolean = false,
    ) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!settings.notificationsEnabled || !nm.areNotificationsEnabled()) {
            clear(context)
            return
        }
        val pins = things.filter {
            isPinned(it) && it.status != ThingStatus.COMPLETED && it.status != ThingStatus.ARCHIVED
        }.sortedWith(
            compareBy<Thing> { it.sortOrder }
                .thenByDescending { it.priority == Priority.HIGH }
                .thenByDescending { it.updatedAt },
        )
        val visible = pins.take(PIN_LIMIT)
        val wanted = visible.map { pinId(it) }.toSet() + setOf(SUMMARY_ID)
        val active = activeIds(context).toSet()
        active.filter { it !in wanted && it != WELCOME_ID }.forEach { nm.cancel(it) }
        val now = System.currentTimeMillis()
        var showing = 0
        visible.forEachIndexed { index, thing ->
            val id = pinId(thing)
            val wasShowing = id in active
            val justPinned = now - thing.updatedAt < REPOST_WINDOW_MS
            if (wasShowing || restoreMissing || justPinned) {
                showPin(context, thing, withThumb = index < 3, settings = settings, overflow = pins.size - PIN_LIMIT)
                showing += 1
            }
        }
        if (showing == 0) nm.cancel(SUMMARY_ID) else showSummary(context, pins)
    }

    fun clear(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        activeIds(context).forEach { nm.cancel(it) }
        nm.cancel(SUMMARY_ID)
        nm.cancel(WELCOME_ID)
    }

    private fun activeIds(context: Context): List<Int> {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return emptyList()
        return manager.activeNotifications.map { it.id }
    }

    fun showJustSaved(context: Context, thing: Thing) {
        if (isPinned(thing)) return
        ensureChannel(context)
        val openPi = openPending(context, thing)
        val pinPi = actionPi(context, ACTION_PIN, thing.id, requestCode(thing, 1))
        val donePi = actionPi(context, ACTION_DONE, thing.id, requestCode(thing, 2))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_memory)
            .setContentTitle("Saved")
            .setContentText(thing.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(thing.title))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setTimeoutAfter(10_000)
            .setOnlyAlertOnce(true)
            .addAction(0, "Open", openPi)
            .addAction(0, "Pin", pinPi)
            .addAction(0, thingActionVerb(thing), donePi)
            .setColor(0xFF2C5C4F.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(pinId(thing) + 1_000_000, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showWelcome(context: Context) {
        ensureChannel(context)
        val openPi = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_memory)
            .setContentTitle(context.getString(R.string.welcome_notif_title))
            .setContentText(context.getString(R.string.welcome_notif_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.welcome_notif_body)))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setSilent(true)
            .setColor(0xFF2C5C4F.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(WELCOME_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun openThing(context: Context, thing: Thing) {
        val view = viewIntent(context, thing)
        runCatching { context.startActivity(view) }.onFailure {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_THING_ID, thing.id)
                },
            )
        }
    }

    fun viewIntent(context: Context, thing: Thing): Intent {
        val file = thing.imageUri?.let { File(it) }?.takeIf { it.exists() }
        val url = thing.sourceUrl?.trim().orEmpty()
        if (url.startsWith("https://") || url.startsWith("geo:")) {
            return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        if (file != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val mime = thing.mimeType
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?: "*/*"
            return Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_THING_ID, thing.id)
        }
    }

    private fun showPin(
        context: Context,
        thing: Thing,
        withThumb: Boolean,
        settings: Settings,
        overflow: Int = 0,
    ) {
        val openPi = openPending(context, thing)
        val donePi = actionPi(context, ACTION_DONE, thing.id, requestCode(thing, 3))
        val laterPi = actionPi(context, ACTION_LATER, thing.id, requestCode(thing, 4))
        val unpinPi = actionPi(context, ACTION_UNPIN, thing.id, requestCode(thing, 8))
        val items = Checklist.parse(thing.checklist).ifEmpty { Checklist.fromNotes(thing.notes) }
        val checkLines = Checklist.linesForNotification(items)
        val body = if (checkLines.isNotEmpty()) {
            checkLines.joinToString("\n")
        } else {
            pinBody(thing)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_memory)
            .setContentTitle(thing.title)
            .setContentText(if (checkLines.isNotEmpty()) checkLines.first() else pinSubtitle(thing))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPi)
            .setDeleteIntent(unpinPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP)
            .setSortKey(thing.sortOrder.toString().padStart(8, '0'))
            .setColor(PinStyle.argb(thing.pinColor))
            .setVisibility(
                if (settings.lockScreenPrivate) NotificationCompat.VISIBILITY_PRIVATE
                else NotificationCompat.VISIBILITY_PUBLIC,
            )
            .setPriority(
                when (thing.priority) {
                    Priority.HIGH -> NotificationCompat.PRIORITY_HIGH
                    Priority.LOW -> NotificationCompat.PRIORITY_LOW
                    else -> NotificationCompat.PRIORITY_DEFAULT
                },
            )
        if (items.any { !it.done }) {
            builder.addAction(0, context.getString(R.string.notif_check), actionPi(context, ACTION_CHECK, thing.id, requestCode(thing, 9)))
            builder.addAction(0, context.getString(R.string.notif_later), laterPi)
            builder.addAction(0, context.getString(R.string.notif_unpin), unpinPi)
        } else {
            builder.addAction(0, thingActionVerb(thing), donePi)
            builder.addAction(0, context.getString(R.string.notif_later), laterPi)
            builder.addAction(0, context.getString(R.string.notif_unpin), unpinPi)
        }
        if (withThumb) thumb(thing)?.let { builder.setLargeIcon(it) }
        try {
            NotificationManagerCompat.from(context).notify(pinId(thing), builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun showSummary(context: Context, pins: List<Thing>) {
        val openPi = PendingIntent.getActivity(
            context,
            SUMMARY_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val extra = if (pins.size > PIN_LIMIT) " · ${pins.size - PIN_LIMIT} more in the app" else ""
        val style = NotificationCompat.InboxStyle().setBigContentTitle("${pins.size} pinned$extra")
        pins.take(7).forEach { style.addLine(it.title) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_memory)
            .setContentTitle("${pins.size} pinned")
            .setContentText(pins.take(3).joinToString(" · ") { it.title } + extra)
            .setStyle(style)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setColor(0xFF2C5C4F.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(SUMMARY_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun openPending(context: Context, thing: Thing): PendingIntent {
        val view = viewIntent(context, thing)
        val isApp = view.component?.className?.contains("MainActivity") == true
        return PendingIntent.getActivity(
            context,
            requestCode(thing, if (isApp) 5 else 6),
            view,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionPi(context: Context, action: String, thingId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_THING_ID, thingId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pinSubtitle(thing: Thing): String =
        listOfNotNull(kindLabel(thing), thing.siteName ?: thing.sourceApp).joinToString(" · ")

    private fun pinBody(thing: Thing): String =
        thing.summary?.takeIf { it.isNotBlank() && it != thing.title } ?: pinSubtitle(thing)

    private fun kindLabel(thing: Thing): String = when (thing.contentType) {
        ContentType.IMAGE -> "Photo"
        ContentType.PDF -> "PDF"
        ContentType.VIDEO -> "Video"
        ContentType.AUDIO -> "Audio"
        ContentType.CONTACT -> "Contact"
        ContentType.LOCATION -> "Place"
        ContentType.FILE -> "File"
        ContentType.URL, ContentType.HTML -> "Link"
        ContentType.TEXT -> "Note"
    }

    private fun thumb(thing: Thing): android.graphics.Bitmap? {
        if (thing.contentType != ContentType.IMAGE) return null
        val path = thing.imageUri ?: return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        } catch (_: Exception) {
            null
        }
    }
}
