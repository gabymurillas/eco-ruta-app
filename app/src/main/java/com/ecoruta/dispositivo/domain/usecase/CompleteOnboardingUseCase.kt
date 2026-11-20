package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.repository.SettingsRepository

/**
 * Marks the welcome carousel as seen. The device state then turns into "unpaired, not first time",
 * so navigation moves on to the pairing code by itself.
 */
class CompleteOnboardingUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke() = settings.markOnboardingSeen()
}
