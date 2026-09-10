package com.secondmemory.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.secondmemory.app.domain.Appearance
import com.secondmemory.app.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    val settings: Flow<Settings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun update(transform: (Settings) -> Settings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.appearance] = next.appearance.name
            prefs[Keys.aiEnabled] = next.aiEnabled
            prefs[Keys.automaticProcessing] = next.automaticProcessing
            prefs[Keys.resurfaceEnabled] = next.resurfaceEnabled
            prefs[Keys.maxNudgesPerDay] = next.maxNudgesPerDay
            prefs[Keys.quietHoursStart] = next.quietHoursStart
            prefs[Keys.quietHoursEnd] = next.quietHoursEnd
            prefs[Keys.workHoursStart] = next.workHoursStart
            prefs[Keys.workHoursEnd] = next.workHoursEnd
            prefs[Keys.readingTime] = next.readingTime
            prefs[Keys.leisureTime] = next.leisureTime
            prefs[Keys.payday] = next.payday
            prefs[Keys.notificationsEnabled] = next.notificationsEnabled
            prefs[Keys.notificationsAsked] = next.notificationsAsked
            prefs[Keys.onboardingComplete] = next.onboardingComplete
            prefs[Keys.nudgesOn] = next.nudgesOn
            prefs[Keys.nudgesToday] = next.nudgesToday
            prefs[Keys.lockScreenPrivate] = next.lockScreenPrivate
            prefs[Keys.pinExpiryHours] = next.pinExpiryHours
        }
    }

    private object Keys {
        val appearance = stringPreferencesKey("appearance")
        val aiEnabled = booleanPreferencesKey("aiEnabled")
        val automaticProcessing = booleanPreferencesKey("automaticProcessing")
        val resurfaceEnabled = booleanPreferencesKey("resurfaceEnabled")
        val maxNudgesPerDay = intPreferencesKey("maxNudgesPerDay")
        val quietHoursStart = stringPreferencesKey("quietHoursStart")
        val quietHoursEnd = stringPreferencesKey("quietHoursEnd")
        val workHoursStart = stringPreferencesKey("workHoursStart")
        val workHoursEnd = stringPreferencesKey("workHoursEnd")
        val readingTime = stringPreferencesKey("readingTime")
        val leisureTime = stringPreferencesKey("leisureTime")
        val payday = intPreferencesKey("payday")
        val notificationsEnabled = booleanPreferencesKey("notificationsEnabled")
        val notificationsAsked = booleanPreferencesKey("notificationsAsked")
        val onboardingComplete = booleanPreferencesKey("onboardingComplete")
        val nudgesOn = stringPreferencesKey("nudgesOn")
        val nudgesToday = intPreferencesKey("nudgesToday")
        val lockScreenPrivate = booleanPreferencesKey("lockScreenPrivate")
        val pinExpiryHours = intPreferencesKey("pinExpiryHours")
    }

    private fun Preferences.toSettings(): Settings {
        val appearanceName = this[Keys.appearance] ?: Appearance.SYSTEM.name
        return Settings(
            appearance = runCatching { Appearance.valueOf(appearanceName) }.getOrDefault(Appearance.SYSTEM),
            aiEnabled = this[Keys.aiEnabled] ?: true,
            automaticProcessing = this[Keys.automaticProcessing] ?: true,
            resurfaceEnabled = this[Keys.resurfaceEnabled] ?: false,
            maxNudgesPerDay = this[Keys.maxNudgesPerDay] ?: 5,
            quietHoursStart = this[Keys.quietHoursStart] ?: "22:00",
            quietHoursEnd = this[Keys.quietHoursEnd] ?: "07:00",
            workHoursStart = this[Keys.workHoursStart] ?: "09:00",
            workHoursEnd = this[Keys.workHoursEnd] ?: "18:00",
            readingTime = this[Keys.readingTime] ?: "19:30",
            leisureTime = this[Keys.leisureTime] ?: "20:30",
            payday = this[Keys.payday] ?: 28,
            notificationsEnabled = this[Keys.notificationsEnabled] ?: false,
            notificationsAsked = this[Keys.notificationsAsked] ?: false,
            onboardingComplete = this[Keys.onboardingComplete] ?: false,
            nudgesOn = this[Keys.nudgesOn] ?: "",
            nudgesToday = this[Keys.nudgesToday] ?: 0,
            lockScreenPrivate = this[Keys.lockScreenPrivate] ?: true,
            pinExpiryHours = this[Keys.pinExpiryHours] ?: 0,
        )
    }
}
