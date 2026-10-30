package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(private val preferences: DevicePreferences) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = preferences.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
}
