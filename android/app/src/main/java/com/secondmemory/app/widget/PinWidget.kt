package com.secondmemory.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.secondmemory.app.MainActivity
import com.secondmemory.app.R
import com.secondmemory.app.domain.Thing
import com.secondmemory.app.domain.ThingStatus
import com.secondmemory.app.notify.NotificationHelper

class PinWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            manager.updateAppWidget(id, views(context, lastCount))
        }
    }

    companion object {
        @Volatile private var lastCount = 0

        fun update(context: Context, things: List<Thing>) {
            lastCount = things.count {
                NotificationHelper.isPinned(it) &&
                    it.status != ThingStatus.COMPLETED &&
                    it.status != ThingStatus.ARCHIVED
            }
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PinWidget::class.java))
            ids.forEach { id -> manager.updateAppWidget(id, views(context, lastCount)) }
        }

        private fun views(context: Context, count: Int): RemoteViews {
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return RemoteViews(context.packageName, R.layout.pin_widget).apply {
                setTextViewText(R.id.widget_count, count.toString())
                setTextViewText(R.id.widget_label, if (count == 1) "pinned" else "pinned")
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }
    }
}
