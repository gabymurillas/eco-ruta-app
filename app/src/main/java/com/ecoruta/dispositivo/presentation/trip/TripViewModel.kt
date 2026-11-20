package com.ecoruta.dispositivo.presentation.trip

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.core.onFailure
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.domain.usecase.FinishTripUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveMotionUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveRouteSignUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveTripStatusUseCase
import com.ecoruta.dispositivo.presentation.common.toMessageRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the trip screen shows: the live [TripStatus], the route and the finish action's progress. */
data class TripUiState(
    val status: TripStatus = TripStatus(),
    val normalRouteName: String = "",
    /** The route name the web shows instead of [normalRouteName]; null: the usual route. */
    val signName: String? = null,
    /** While moving, changing the route is locked: the driver is driving. */
    val isMoving: Boolean = false,
    val isFinishing: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

class TripViewModel(
    observeTripStatus: ObserveTripStatusUseCase,
    observeDeviceState: ObserveDeviceStateUseCase,
    observeRouteSign: ObserveRouteSignUseCase,
    observeMotion: ObserveMotionUseCase,
    private val finishTrip: FinishTripUseCase,
) : ViewModel() {

    private val action = MutableStateFlow(FinishAction())

    private val normalRouteName = observeDeviceState().map { (it as? DeviceState.OnTrip)?.session?.routeName.orEmpty() }

    val uiState: StateFlow<TripUiState> = combine(
        observeTripStatus(),
        action,
        normalRouteName,
        observeRouteSign(),
        observeMotion(),
    ) { status, finish, routeName, sign, moving ->
        TripUiState(
            status = status,
            normalRouteName = routeName,
            signName = sign?.name,
            isMoving = moving,
            isFinishing = finish.isFinishing,
            errorRes = finish.errorRes,
        )
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
