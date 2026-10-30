package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Settings live in the same DataStore as the device identity: one file, one source of truth. */
class SettingsRepositoryImpl(private val preferences: DevicePreferences) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = preferences.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
}
