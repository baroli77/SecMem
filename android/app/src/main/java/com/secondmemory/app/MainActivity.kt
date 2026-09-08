package com.secondmemory.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondmemory.app.notify.NotificationHelper
import com.secondmemory.app.ui.MemoryViewModel
import com.secondmemory.app.ui.nav.SecondMemoryAppUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val openThingId = mutableStateOf<String?>(null)
    private val openCapture = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOpen(intent)
        val repo = (application as SecondMemoryApp).container.repository
        setContent {
            val vm: MemoryViewModel = viewModel(factory = MemoryViewModel.factory(repo))
            val openId by openThingId
            val capture by openCapture
            SecondMemoryAppUi(vm = vm, initialThingId = openId, openCapture = capture, onCaptureConsumed = { openCapture.value = false })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpen(intent)
    }

    private fun handleOpen(intent: Intent?) {
        val id = intent?.getStringExtra(NotificationHelper.EXTRA_THING_ID)
        openThingId.value = id
        if (intent?.getBooleanExtra("openCapture", false) == true) {
            openCapture.value = true
        }
        if (id.isNullOrBlank() || intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_CONTENT, false) != true) return
        val repo = (application as SecondMemoryApp).container.repository
        lifecycleScope.launch {
            val thing = withContext(Dispatchers.IO) {
                repo.currentThings().firstOrNull { it.id == id }
            } ?: return@launch
            NotificationHelper.openThing(this@MainActivity, thing)
        }
    }
}