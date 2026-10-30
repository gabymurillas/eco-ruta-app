package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** The theme mode chosen on screen; follows the phone until someone picks another. */
class ObserveThemeModeUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = settings.themeMode
}
