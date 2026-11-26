package com.example.ui.state

import com.example.data.local.ControllerConfigEntity
import com.example.data.local.IrrigationScheduleEntity
import com.example.data.local.IrrigationZoneEntity
import com.example.data.local.WateringLogEntity
import com.example.data.remote.ControllerPingResult
import com.example.data.repository.WeatherSnapshot

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot) : WeatherUiState
    data class Error(val message: String, val cached: WeatherSnapshot? = null) : WeatherUiState
}

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Success(val result: ControllerPingResult) : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

data class DailyWaterChartBar(
    val dayLabel: String,
    val liters: Float
)

data class IrrigationUiState(
    val zones: List<IrrigationZoneEntity> = emptyList(),
    val schedules: List<IrrigationScheduleEntity> = emptyList(),
    val logs: List<WateringLogEntity> = emptyList(),
    val config: ControllerConfigEntity = ControllerConfigEntity(),
    val weatherState: WeatherUiState = WeatherUiState.Loading,
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle,
    val weeklyWaterChart: List<DailyWaterChartBar> = emptyList(),
    val todayTotalLiters: Float = 0f,
    val weekTotalLiters: Float = 0f,
    val activeZoneCount: Int = 0,
    val averageMoisturePercent: Int = 0,
    val shouldRainSkipNow: Boolean = false,
    val snackbarMessage: String? = null
)
