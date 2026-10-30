package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Device-local preferences chosen on screen. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
