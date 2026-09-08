@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.secondmemory.app.ui.nav

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secondmemory.app.notify.NotificationHelper
import com.secondmemory.app.notify.ReminderScheduler
import com.secondmemory.app.ui.MemoryViewModel
import com.secondmemory.app.ui.components.CaptureSheet
import com.secondmemory.app.ui.screens.DetailScreen
import com.secondmemory.app.ui.screens.LibraryScreen
import com.secondmemory.app.ui.screens.OnboardingScreen
import com.secondmemory.app.ui.screens.SearchScreen
import com.secondmemory.app.ui.screens.SettingsScreen
import com.secondmemory.app.ui.screens.TodayScreen
import com.secondmemory.app.ui.theme.SecondMemoryTheme
import kotlinx.coroutines.launch

@Composable
fun SecondMemoryAppUi(
    vm: MemoryViewModel,
    initialThingId: String? = null,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    val things by vm.things.collectAsStateWithLifecycle()
    SecondMemoryTheme(appearance = settings.appearance) {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var captureOpen by remember { mutableStateOf(false) }
        var onboardingDoneLocal by remember { mutableStateOf(false) }
        var showNotifPrompt by remember { mutableStateOf(false) }
        val onboardingDone = settings.onboardingComplete || onboardingDoneLocal
        val route = nav.currentBackStackEntryAsState().value?.destination?.route

        LaunchedEffect(things, settings.notificationsEnabled) {
            if (settings.notificationsEnabled) {
                NotificationHelper.refreshPins(context, things)
            } else {
                NotificationHelper.clear(context)
            }
            ReminderScheduler.scheduleNext(context, things)
        }

        fun markDone(id: String) {
            vm.complete(id)
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = "Done",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) vm.restore(id)
            }
        }

        fun pinToToday(id: String) {
            val thing = things.firstOrNull { it.id == id }
            val turningOn = thing != null && !(thing.isPinned || thing.isFavourite)
            vm.togglePin(id)
            if (turningOn) {
                scope.launch { snackbar.showSnackbar("On Today") }
            }
        }

        val permission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            vm.patchSettings { it.copy(notificationsEnabled = granted, notificationsAsked = true) }
            if (granted) NotificationHelper.showWelcome(context)
        }

        fun requestNotifications() {
            if (Build.VERSION.SDK_INT >= 33) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                vm.patchSettings { it.copy(notificationsEnabled = true, notificationsAsked = true) }
                NotificationHelper.showWelcome(context)
            }
        }

        LaunchedEffect(onboardingDone, settings.notificationsAsked) {
            if (!onboardingDone || settings.notificationsAsked) return@LaunchedEffect
            val alreadyGranted = if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (alreadyGranted) {
                vm.patchSettings { it.copy(notificationsEnabled = true, notificationsAsked = true) }
            } else {
                showNotifPrompt = true
            }
        }
        LaunchedEffect(initialThingId) {
            if (!initialThingId.isNullOrBlank()) nav.navigate("thing/$initialThingId")
        }

        if (!settingsReady) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
            return@SecondMemoryTheme
        }

        if (!onboardingDone) {
            OnboardingScreen(
                onFinished = {
                    onboardingDoneLocal = true
                    vm.completeOnboarding()
                },
            )
            return@SecondMemoryTheme
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (route != "onboarding" && route?.startsWith("thing/") != true) {
                    androidx.compose.material3.TopAppBar(
                        title = { Text("Second Memory", style = MaterialTheme.typography.titleLarge) },
                        actions = {
                            IconButton(onClick = { nav.navigate("search") }) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { nav.navigate("settings") }) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            },
            bottomBar = {
                if (route != "onboarding" && route?.startsWith("thing/") != true) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = route == "today",
                            onClick = { nav.tab("today") },
                            icon = { Icon(Icons.Outlined.Today, contentDescription = "Today") },
                            label = { Text("Today") },
                        )
                        NavigationBarItem(
                            selected = route == "library",
                            onClick = { nav.tab("library") },
                            icon = { Icon(Icons.Outlined.Folder, contentDescription = "Library") },
                            label = { Text("Library") },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (route != "onboarding" && route?.startsWith("thing/") != true) {
                    FloatingActionButton(
                        onClick = { captureOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Save")
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "today",
                modifier = Modifier.padding(padding),
            ) {
                composable("today") {
                    TodayScreen(
                        things = things,
                        settings = settings,
                        onOpen = { nav.navigate("thing/$it") },
                        onDone = ::markDone,
                        onSnooze = vm::snooze,
                        onArchive = vm::archive,
                        onPin = ::pinToToday,
                    )
                }
                composable("library") {
                    LibraryScreen(
                        things = things,
                        settings = settings,
                        onOpen = { nav.navigate("thing/$it") },
                        onDone = ::markDone,
                        onSnooze = vm::snooze,
                        onArchive = vm::archive,
                        onPin = ::pinToToday,
                    )
                }
                composable("search") {
                    SearchScreen(
                        things = things,
                        settings = settings,
                        onOpen = { nav.navigate("thing/$it") },
                        onDone = ::markDone,
                        onSnooze = vm::snooze,
                        onArchive = vm::archive,
                        onPin = ::pinToToday,
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        settings = settings,
                        things = things,
                        onPatch = vm::patchSettings,
                        onLoadExamples = vm::loadExamples,
                        onReset = vm::resetAll,
                    )
                }
                composable(
                    "thing/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString("id")
                    val thing = things.firstOrNull { it.id == id }
                    DetailScreen(
                        thing = thing,
                        settings = settings,
                        onBack = { nav.popBackStack() },
                        onOpen = { id?.let(vm::openThing) },
                        onDone = { id?.let(::markDone); nav.popBackStack() },
                        onSnooze = { until -> id?.let { vm.snooze(it, until) } },
                        onArchive = { id?.let(vm::archive); nav.popBackStack() },
                        onRestore = { id?.let(vm::restore) },
                        onDelete = { id?.let(vm::remove); nav.popBackStack() },
                        onPin = { id?.let(::pinToToday) },
                        onNotes = { notes -> id?.let { vm.updateNotes(it, notes) } },
                        onTitle = { title -> id?.let { vm.updateTitle(it, title) } },
                        onCategory = { cat -> id?.let { vm.updateCategory(it, cat) } },
                    )
                }
            }
        }

        if (captureOpen) {
            CaptureSheet(
                onDismiss = { captureOpen = false },
                onCapture = { input ->
                    vm.capture(input) { result ->
                        scope.launch {
                            val msg = when {
                                result.blocked -> "Free limit reached"
                                result.duplicate != null -> "Already saved"
                                else -> "Saved"
                            }
                            snackbar.showSnackbar(msg)
                        }
                    }
                },
            )
        }

        if (showNotifPrompt) {
            AlertDialog(
                onDismissRequest = {
                    showNotifPrompt = false
                    vm.patchSettings { it.copy(notificationsAsked = true) }
                },
                title = { Text("Stay in the shade?") },
                text = {
                    Text(
                        "Pinned things sit in your notification shade until you unpin them. " +
                            "Share a link, a photo, a PDF — it stays put. You can turn this off in Settings.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showNotifPrompt = false
                            requestNotifications()
                        },
                    ) { Text("Allow") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNotifPrompt = false
                            vm.patchSettings { it.copy(notificationsAsked = true, notificationsEnabled = false) }
                        },
                    ) { Text("Not now") }
                },
            )
        }
    }
}

private fun androidx.navigation.NavHostController.tab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
