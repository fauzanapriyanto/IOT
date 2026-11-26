package com.example.ui.state

import com.example.data.local.ControllerConfigEntity
import com.example.data.remote.GanApiPingResult
import com.example.data.remote.GanControlConfigDto
import com.example.data.remote.GanSensorReadingDto
import com.example.data.remote.GanSummaryData

sealed interface GanApiTestState {
    data object Idle : GanApiTestState
    data object Testing : GanApiTestState
    data class Success(val result: GanApiPingResult) : GanApiTestState
    data class Error(val message: String) : GanApiTestState
}

data class GanUiState(
    val config: ControllerConfigEntity = ControllerConfigEntity(),
    val isServerOnline: Boolean = false,
    val isLoadingLatest: Boolean = true,
    val isLoadingHistory: Boolean = false,
    val isSavingControl: Boolean = false,
    val isSubmittingSensor: Boolean = false,
    val latestReading: GanSensorReadingDto? = null,
    val historyRows: List<GanSensorReadingDto> = emptyList(),
    val summary: GanSummaryData = GanSummaryData(),
    val controlConfig: GanControlConfigDto = GanControlConfigDto(),
    val controlLoadedFromServer: Boolean = false,
    val lastErrorMessage: String? = null,
    val controlStatusMessage: String? = null,
    val apiTestState: GanApiTestState = GanApiTestState.Idle,
    val weatherState: WeatherUiState = WeatherUiState.Loading,
    val snackbarMessage: String? = null
)
