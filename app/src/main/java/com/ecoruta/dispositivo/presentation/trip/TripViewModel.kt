package com.ecoruta.dispositivo.presentation.trip

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.core.onFailure
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.domain.usecase.FinishTripUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveTripStatusUseCase
import com.ecoruta.dispositivo.presentation.common.toMessageRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the trip screen shows: the live [TripStatus] plus the finish action's progress. */
data class TripUiState(
    val status: TripStatus = TripStatus(),
    val isFinishing: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

class TripViewModel(
    observeTripStatus: ObserveTripStatusUseCase,
    private val finishTrip: FinishTripUseCase,
) : ViewModel() {

    private val action = MutableStateFlow(FinishAction())

    val uiState: StateFlow<TripUiState> = combine(observeTripStatus(), action) { status, finish ->
        TripUiState(status, finish.isFinishing, finish.errorRes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TripUiState())

    fun onFinishTrip() {
        if (action.value.isFinishing) return

        action.value = FinishAction(isFinishing = true)
        viewModelScope.launch {
            // On success the active trip is cleared and navigation returns to the idle screen.
            finishTrip().onFailure { error ->
                action.update { it.copy(errorRes = error.toMessageRes()) }
            }
            action.update { it.copy(isFinishing = false) }
        }
    }

    private data class FinishAction(
        val isFinishing: Boolean = false,
        @StringRes val errorRes: Int? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
