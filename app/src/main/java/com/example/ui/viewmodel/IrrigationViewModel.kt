package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ControllerConfigEntity
import com.example.data.local.IrrigationDatabase
import com.example.data.local.IrrigationScheduleEntity
import com.example.data.local.IrrigationZoneEntity
import com.example.data.local.WateringLogEntity
import com.example.data.remote.IoTControllerClient
import com.example.data.remote.LocationPreset
import com.example.data.remote.OpenMeteoService
import com.example.data.repository.IrrigationRepository
import com.example.data.repository.WeatherSnapshot
import com.example.ui.state.ConnectionTestState
import com.example.ui.state.DailyWaterChartBar
import com.example.ui.state.IrrigationUiState
import com.example.ui.state.WeatherUiState
import com.example.ui.util.IrrigationFormatters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IrrigationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = IrrigationDatabase.getInstance(application)
    private val repository = IrrigationRepository(
        zoneDao = db.zoneDao(),
        scheduleDao = db.scheduleDao(),
        logDao = db.wateringLogDao(),
        configDao = db.controllerConfigDao(),
        weatherService = OpenMeteoService.create(),
        iotClient = IoTControllerClient()
    )

    private val weatherStateFlow = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    private val connectionTestFlow = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    private val snackbarFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<IrrigationUiState> = combine(
        repository.zonesFlow,
        repository.schedulesFlow,
        repository.logsFlow,
        repository.configFlow,
        weatherStateFlow,
        connectionTestFlow,
        snackbarFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val zones = values[0] as List<IrrigationZoneEntity>
        @Suppress("UNCHECKED_CAST")
        val schedules = values[1] as List<IrrigationScheduleEntity>
        @Suppress("UNCHECKED_CAST")
        val logs = values[2] as List<WateringLogEntity>
        val config = values[3] as ControllerConfigEntity
        val weatherState = values[4] as WeatherUiState
        val connState = values[5] as ConnectionTestState
        val snack = values[6] as String?

        val chart = buildWeeklyWaterChart(logs)
        val todayStart = IrrigationFormatters.getStartOfDayMillis(0)
        val weekStart = IrrigationFormatters.getStartOfDayMillis(6)

        val todayLiters = logs.filter { it.startedAt >= todayStart }.sumOf { it.litersUsed.toDouble() }.toFloat()
        val weekLiters = logs.filter { it.startedAt >= weekStart }.sumOf { it.litersUsed.toDouble() }.toFloat()
        val avgMoisture = if (zones.isEmpty()) 0 else zones.sumOf { it.soilMoisturePercent } / zones.size
        val activeCount = zones.count { it.isValveOpen }

        val snapshot = when (weatherState) {
            is WeatherUiState.Success -> weatherState.snapshot
            is WeatherUiState.Error -> weatherState.cached
            WeatherUiState.Loading -> null
        }
        val rainSkipTriggered = config.rainSkipEnabled &&
            snapshot != null &&
            snapshot.nextHoursRainProbability >= config.rainThresholdPercent

        IrrigationUiState(
            zones = zones,
            schedules = schedules,
            logs = logs,
            config = config,
            weatherState = weatherState,
            connectionTestState = connState,
            weeklyWaterChart = chart,
            todayTotalLiters = todayLiters,
            weekTotalLiters = weekLiters,
            activeZoneCount = activeCount,
            averageMoisturePercent = avgMoisture,
            shouldRainSkipNow = rainSkipTriggered,
            snackbarMessage = snack
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IrrigationUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val cfg = repository.configFlow.first()
            val cached = repository.parseCachedWeather(cfg)
            if (cached != null) {
                weatherStateFlow.value = WeatherUiState.Success(cached)
            }
            val stale = cfg.lastWeatherSyncAt == null ||
                (System.currentTimeMillis() - cfg.lastWeatherSyncAt) > 20 * 60_000L
            if (stale) {
                refreshWeather(cfg.latitude, cfg.longitude, cfg.locationName, silentIfCached = cached != null)
            }
        }

        // Active 1-second valve countdown & moisture simulation ticker
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (uiState.value.activeZoneCount > 0) {
                    repository.tickActiveZonesSimulationAndCountdown()
                }
            }
        }
    }

    fun refreshWeather(
        latitude: Double = uiState.value.config.latitude,
        longitude: Double = uiState.value.config.longitude,
        locationName: String = uiState.value.config.locationName,
        silentIfCached: Boolean = false
    ) {
        viewModelScope.launch {
            val previousCached = when (val current = weatherStateFlow.value) {
                is WeatherUiState.Success -> current.snapshot
                is WeatherUiState.Error -> current.cached
                else -> repository.parseCachedWeather(uiState.value.config)
            }
            if (!silentIfCached) {
                weatherStateFlow.value = WeatherUiState.Loading
            }
            val result = repository.refreshWeather(latitude, longitude, locationName)
            result.onSuccess { snapshot ->
                weatherStateFlow.value = WeatherUiState.Success(snapshot)
            }.onFailure { err ->
                weatherStateFlow.value = WeatherUiState.Error(
                    message = err.localizedMessage ?: "Gagal memuat data cuaca Open-Meteo",
                    cached = previousCached
                )
            }
        }
    }

    fun selectLocationPreset(preset: LocationPreset) {
        refreshWeather(preset.latitude, preset.longitude, preset.name)
        showMessage("Lokasi cuaca diatur ke ${preset.name}")
    }

    @SuppressLint("MissingPermission")
    fun useDeviceGpsLocation() {
        val context = getApplication<Application>()
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        weatherStateFlow.value = WeatherUiState.Loading
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch {
                        val placeName = resolveCityName(location.latitude, location.longitude)
                        refreshWeather(location.latitude, location.longitude, placeName)
                        showMessage("Lokasi GPS diperbarui: $placeName")
                    }
                } else {
                    val cfg = uiState.value.config
                    refreshWeather(cfg.latitude, cfg.longitude, cfg.locationName)
                    showMessage("Sinyal GPS tidak tersedia, menggunakan ${cfg.locationName}")
                }
            }
            .addOnFailureListener {
                val cfg = uiState.value.config
                refreshWeather(cfg.latitude, cfg.longitude, cfg.locationName)
                showMessage("Gagal membaca GPS perangkat.")
            }
    }

    private suspend fun resolveCityName(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(getApplication(), Locale("id", "ID"))
            @Suppress("DEPRECATION")
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1)
            } else {
                geocoder.getFromLocation(lat, lon, 1)
            }
            val addr = addresses?.firstOrNull()
            val city = addr?.subAdminArea ?: addr?.locality ?: addr?.adminArea
            if (!city.isNullOrBlank()) city else String.format(Locale.US, "%.3f, %.3f", lat, lon)
        } catch (_: Exception) {
            String.format(Locale.US, "%.3f, %.3f", lat, lon)
        }
    }

    fun startZoneWatering(zone: IrrigationZoneEntity, durationMinutes: Int) {
        viewModelScope.launch {
            val res = repository.startZoneWatering(zone, durationMinutes, "Manual")
            res.onSuccess { showMessage(it) }
                .onFailure { showMessage(it.localizedMessage ?: "Gagal membuka katup") }
        }
    }

    fun stopZoneWatering(zone: IrrigationZoneEntity) {
        viewModelScope.launch {
            val res = repository.stopZoneWatering(zone, completedNaturally = false)
            res.onSuccess { showMessage(it) }
        }
    }

    fun startAllZones(durationMinutes: Int = 5) {
        viewModelScope.launch {
            val res = repository.startAllZones(durationMinutes)
            res.onSuccess { showMessage(it) }
                .onFailure { showMessage(it.localizedMessage ?: "Gagal memulai penyiraman massal") }
        }
    }

    fun stopAllZones() {
        viewModelScope.launch {
            val stopped = repository.stopAllZones()
            showMessage(
                if (stopped > 0) "Stop darurat: $stopped katup ditutup."
                else "Tidak ada katup yang sedang menyiram."
            )
        }
    }

    fun runSmartAutomationCheck() {
        viewModelScope.launch {
            val snapshot: WeatherSnapshot? = when (val ws = weatherStateFlow.value) {
                is WeatherUiState.Success -> ws.snapshot
                is WeatherUiState.Error -> ws.cached
                WeatherUiState.Loading -> null
            }
            val summary = repository.evaluateSmartAutomation(snapshot)
            showMessage(summary)
        }
    }

    fun saveZone(zone: IrrigationZoneEntity) {
        viewModelScope.launch {
            repository.saveZone(zone)
            showMessage("Zona \"${zone.name}\" disimpan.")
        }
    }

    fun deleteZone(zone: IrrigationZoneEntity) {
        viewModelScope.launch {
            repository.deleteZone(zone)
            showMessage("Zona \"${zone.name}\" dihapus.")
        }
    }

    fun adjustSimulationMoisture(zone: IrrigationZoneEntity, delta: Int) {
        viewModelScope.launch {
            repository.adjustSimulationMoisture(zone, delta)
        }
    }

    fun saveSchedule(schedule: IrrigationScheduleEntity) {
        viewModelScope.launch {
            repository.saveSchedule(schedule)
            showMessage("Jadwal \"${schedule.label}\" disimpan.")
        }
    }

    fun toggleSchedule(schedule: IrrigationScheduleEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSchedule(schedule, enabled)
        }
    }

    fun deleteSchedule(schedule: IrrigationScheduleEntity) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            showMessage("Jadwal \"${schedule.label}\" dihapus.")
        }
    }

    fun updateControllerSettings(
        url: String,
        token: String,
        isSimulationMode: Boolean
    ) {
        viewModelScope.launch {
            repository.updateConfig {
                it.copy(
                    controllerUrl = url.trim(),
                    apiToken = token.trim(),
                    isSimulationMode = isSimulationMode
                )
            }
            showMessage(
                if (isSimulationMode) "Mode Simulasi Perangkat diaktifkan."
                else "Konfigurasi kontroler fisik disimpan."
            )
        }
    }

    fun setRainSkipSettings(enabled: Boolean, thresholdPercent: Int) {
        viewModelScope.launch {
            repository.updateConfig {
                it.copy(
                    rainSkipEnabled = enabled,
                    rainThresholdPercent = thresholdPercent.coerceIn(20, 95)
                )
            }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(useDynamicColor = enabled) }
        }
    }

    fun testHardwareConnection(url: String, token: String) {
        viewModelScope.launch {
            connectionTestFlow.value = ConnectionTestState.Testing
            val res = repository.testHardwareConnection(url, token)
            connectionTestFlow.value = res.fold(
                onSuccess = { ConnectionTestState.Success(it) },
                onFailure = { ConnectionTestState.Error(it.localizedMessage ?: "Koneksi gagal") }
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            showMessage("Semua riwayat penyiraman dibersihkan.")
        }
    }

    fun consumeSnackbar() {
        snackbarFlow.value = null
    }

    private fun showMessage(msg: String) {
        snackbarFlow.value = msg
    }

    private fun buildWeeklyWaterChart(logs: List<WateringLogEntity>): List<DailyWaterChartBar> {
        val dayFmt = SimpleDateFormat("EEE", Locale("id", "ID"))
        return (6 downTo 0).map { daysAgo ->
            val start = IrrigationFormatters.getStartOfDayMillis(daysAgo)
            val end = start + 24 * 3600_000L
            val liters = logs
                .filter { it.startedAt in start until end }
                .sumOf { it.litersUsed.toDouble() }
                .toFloat()
            val label = if (daysAgo == 0) "Hari Ini" else dayFmt.format(Date(start))
            DailyWaterChartBar(dayLabel = label, liters = liters)
        }
    }
}
