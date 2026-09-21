package com.ecoruta.dispositivo.presentation.pairing

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.dispositivo.core.onFailure
import com.ecoruta.dispositivo.domain.usecase.PairDeviceUseCase
import com.ecoruta.dispositivo.presentation.common.toMessageRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

class PairingViewModel(private val pairDevice: PairDeviceUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(code = code, errorRes = null) }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            // On success the persisted session changes and navigation moves on by itself.
            pairDevice(current.code).onFailure { error ->
                _uiState.update { it.copy(errorRes = error.toMessageRes()) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
