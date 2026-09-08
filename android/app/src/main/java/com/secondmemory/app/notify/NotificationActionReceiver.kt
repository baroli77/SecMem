package com.secondmemory.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationHelper.EXTRA_THING_ID) ?: return
        val action = intent.action ?: return
        NotificationActionWorker.enqueue(context, action, id)
    }
}
