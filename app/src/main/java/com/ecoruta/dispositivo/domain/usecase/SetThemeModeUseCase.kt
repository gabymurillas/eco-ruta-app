package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.repository.SettingsRepository

/** Persists the theme mode picked in the top bar selector, so it survives a restart. */
class SetThemeModeUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}
