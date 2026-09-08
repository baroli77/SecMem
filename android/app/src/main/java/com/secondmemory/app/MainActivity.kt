package com.secondmemory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondmemory.app.notify.NotificationHelper
import com.secondmemory.app.ui.MemoryViewModel
import com.secondmemory.app.ui.nav.SecondMemoryAppUi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as SecondMemoryApp).container.repository
        setContent {
            val vm: MemoryViewModel = viewModel(factory = MemoryViewModel.factory(repo))
            val openId = intent?.getStringExtra(NotificationHelper.EXTRA_THING_ID)
            LaunchedEffect(openId) {
                // Navigation is handled inside the app UI via initialThingId.
            }
            SecondMemoryAppUi(vm = vm, initialThingId = openId)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
