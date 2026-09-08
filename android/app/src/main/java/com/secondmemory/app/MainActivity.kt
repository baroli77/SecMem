package com.secondmemory.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondmemory.app.notify.NotificationHelper
import com.secondmemory.app.ui.MemoryViewModel
import com.secondmemory.app.ui.nav.SecondMemoryAppUi

class MainActivity : ComponentActivity() {
    private val openThingId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openThingId.value = intent?.getStringExtra(NotificationHelper.EXTRA_THING_ID)
        val repo = (application as SecondMemoryApp).container.repository
        setContent {
            val vm: MemoryViewModel = viewModel(factory = MemoryViewModel.factory(repo))
            val openId by openThingId
            SecondMemoryAppUi(vm = vm, initialThingId = openId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openThingId.value = intent.getStringExtra(NotificationHelper.EXTRA_THING_ID)
    }
}
