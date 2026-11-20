package com.ecoruta.dispositivo.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.launch

/**
 * The welcome carousel only records that it was seen. Navigation follows the device state: once
 * marked, the phone is "unpaired, not new" and the pairing code shows by itself.
 */
class OnboardingViewModel(private val completeOnboarding: CompleteOnboardingUseCase) : ViewModel() {

    private var finishing = false

    fun onFinish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch { completeOnboarding() }
    }
}
