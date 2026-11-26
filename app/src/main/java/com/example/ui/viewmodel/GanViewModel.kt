package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ControllerConfigEntity
import com.example.data.local.IrrigationDatabase
import com.example.data.remote.GanApiService
import com.example.data.remote.GanControlConfigDto
import com.example.data.remote.GanInsertPayload
import com.example.data.remote.GanSensorReadingDto
import com.example.data.remote.GanSummaryData
import com.example.data.remote.IoTControllerClient
import com.example.data.remote.LocationPreset
import com.example.data.remote.OpenMeteoService
import com.example.data.repository.IrrigationRepository
import com.example.ui.state.GanApiTestState
import com.example.ui.state.GanUiState
import com.example.ui.state.WeatherUiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GanViewModel(application: Application) : AndroidViewModel(application) {
    private val db = IrrigationDatabase.getInstance(application)
    private val configDao = db.controllerConfigDao()
    private val ganApi = GanApiService()
    private val weatherRepo = IrrigationRepository(
        zoneDao = db.zoneDao(),
        scheduleDao = db.scheduleDao(),
        logDao = db.wateringLogDao(),
        configDao = configDao,
        weatherService = OpenMeteoService.create(),
        iotClient = IoTControllerClient()
    )

    private val _uiState = MutableStateFlow(GanUiState())
    val uiState: StateFlow<GanUiState> = _uiState.asStateFlow()

    private var lastKnownSensorId: Long? = null

    init {
        viewModelScope.launch {
            if (configDao.getConfigOnce() == null) {
                configDao.upsertConfig(ControllerConfigEntity())
            }
            val initialCfg = configDao.getConfigOnce() ?: ControllerConfigEntity()
            _uiState.update {
                it.copy(
                    config = initialCfg,
                    controlConfig = it.controlConfig.copy(deviceId = initialCfg.ganDeviceId)
                )
            }

            // Load cached weather if any
            val cachedWeather = weatherRepo.parseCachedWeather(initialCfg)
            if (cachedWeather != null) {
                _uiState.update { it.copy(weatherState = WeatherUiState.Success(cachedWeather)) }
            }

            // Initial load matching index.php $(document).ready
            refreshAllData(forceReloadControl = true)
            refreshWeather(
                initialCfg.latitude,
                initialCfg.longitude,
                initialCfg.locationName,
                silentIfCached = cachedWeather != null
            )

            // Observe config changes
            launch {
                configDao.observeConfig().collect { cfg ->
                    if (cfg != null) {
                        _uiState.update { it.copy(config = cfg) }
                    }
                }
            }
        }

        // Poll latest sensor every 5 seconds (matching index.php setInterval(loadLatest, 5000))
        viewModelScope.launch {
            while (isActive) {
                delay(5000L)
                loadLatest(triggerHistoryOnNewId = true)
            }
        }

        // Refresh history & summary every 30 seconds (matching index.php setInterval(loadHistory, 30000))
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                loadHistory()
                loadSummary()
            }
        }
    }

    fun refreshAllData(forceReloadControl: Boolean = false) {
        viewModelScope.launch {
            loadLatest(triggerHistoryOnNewId = false)
            loadControl(forceOverwrite = forceReloadControl || !_uiState.value.controlLoadedFromServer)
            loadHistory()
            loadSummary()
        }
    }

    fun loadLatest(triggerHistoryOnNewId: Boolean = true) {
        viewModelScope.launch {
            val cfg = _uiState.value.config
            val res = ganApi.fetchLatest(cfg.ganApiUrl, cfg.ganDeviceId)
            res.onSuccess { reading ->
                if (reading != null) {
                    val isNewId = lastKnownSensorId != null && reading.id != lastKnownSensorId
                    lastKnownSensorId = reading.id
                    _uiState.update {
                        it.copy(
                            isServerOnline = true,
                            isLoadingLatest = false,
                            latestReading = reading,
                            lastErrorMessage = null
                        )
                    }
                    if (triggerHistoryOnNewId && isNewId) {
                        loadHistory()
                        loadSummary()
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isServerOnline = true,
                            isLoadingLatest = false,
                            lastErrorMessage = "Belum ada data sensor untuk ${cfg.ganDeviceId}"
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isServerOnline = false,
                        isLoadingLatest = false,
                        lastErrorMessage = err.localizedMessage ?: "Gagal terhubung ke server API"
                    )
                }
            }
        }
    }

    fun loadHistory(limit: Int = 100) {
        viewModelScope.launch {
            val cfg = _uiState.value.config
            _uiState.update { it.copy(isLoadingHistory = true) }
            val res = ganApi.fetchHistory(cfg.ganApiUrl, cfg.ganDeviceId, limit)
            res.onSuccess { rows ->
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        historyRows = rows,
                        isServerOnline = true
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            val cfg = _uiState.value.config
            val res = ganApi.fetchSummary(cfg.ganApiUrl, cfg.ganDeviceId)
            res.onSuccess { summary ->
                _uiState.update { it.copy(summary = summary) }
            }
        }
    }

    fun loadControl(forceOverwrite: Boolean = false) {
        viewModelScope.launch {
            if (!forceOverwrite && _uiState.value.controlLoadedFromServer) return@launch
            val cfg = _uiState.value.config
            val res = ganApi.fetchControl(cfg.ganApiUrl, cfg.ganDeviceId)
            res.onSuccess { ctrl ->
                _uiState.update {
                    it.copy(
                        controlConfig = ctrl,
                        controlLoadedFromServer = true
                    )
                }
            }
        }
    }

    fun updateLocalControlDraft(transform: (GanControlConfigDto) -> GanControlConfigDto) {
        _uiState.update { state ->
            state.copy(
                controlConfig = transform(state.controlConfig),
                controlStatusMessage = null
            )
        }
    }

    fun saveControlToServer() {
        val draft = _uiState.value.controlConfig.copy(deviceId = _uiState.value.config.ganDeviceId)
        val timeRegex = Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")

        if (draft.waterOnAt < 0 || draft.waterOnAt > 100 ||
            draft.waterOffAt < 0 || draft.waterOffAt > 100 ||
            draft.waterOnAt >= draft.waterOffAt ||
            draft.soilOnAt < 0 || draft.soilOnAt > 100 ||
            draft.soilOffAt < 0 || draft.soilOffAt > 100 ||
            draft.soilOnAt >= draft.soilOffAt
        ) {
            val msg = "Periksa ambang batas otomatis: nilai ON harus lebih kecil dari OFF (0–100%)."
            _uiState.update { it.copy(controlStatusMessage = msg, snackbarMessage = msg) }
            return
        }

        if (!timeRegex.matches(draft.timerStart) || !timeRegex.matches(draft.timerEnd)) {
            val msg = "Format jam timer harus HH:mm (contoh: 06:00)."
            _uiState.update { it.copy(controlStatusMessage = msg, snackbarMessage = msg) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingControl = true, controlStatusMessage = null) }
            val res = ganApi.updateControl(_uiState.value.config.ganApiUrl, draft)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavingControl = false,
                        controlLoadedFromServer = false,
                        controlStatusMessage = "Pengaturan berhasil disimpan. ESP32 akan mengambil perintah berikutnya.",
                        snackbarMessage = "Kontrol pompa berhasil dikirim ke api.php"
                    )
                }
                loadControl(forceOverwrite = true)
            }.onFailure { err ->
                val msg = "Gagal menyimpan: ${err.localizedMessage ?: "Kesalahan jaringan"}"
                _uiState.update {
                    it.copy(
                        isSavingControl = false,
                        controlStatusMessage = msg,
                        snackbarMessage = msg
                    )
                }
            }
        }
    }

    fun submitSensorData(payload: GanInsertPayload) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingSensor = true) }
            val cfg = _uiState.value.config
            val res = ganApi.insertSensorData(
                apiUrl = cfg.ganApiUrl,
                payloadData = payload.copy(deviceId = cfg.ganDeviceId)
            )
            res.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        isSubmittingSensor = false,
                        snackbarMessage = "Berhasil dikirim ke api.php: $msg"
                    )
                }
                loadLatest(triggerHistoryOnNewId = false)
                loadHistory()
                loadSummary()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingSensor = false,
                        snackbarMessage = "Gagal kirim data: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun saveApiConnectionConfig(apiUrl: String, deviceId: String) {
        val cleanUrl = apiUrl.trim().ifBlank { GanApiService.DEFAULT_API_URL }
        val cleanDevice = deviceId.trim().ifBlank { GanApiService.DEFAULT_DEVICE_ID }
        viewModelScope.launch {
            val current = configDao.getConfigOnce() ?: ControllerConfigEntity()
            configDao.upsertConfig(
                current.copy(
                    ganApiUrl = cleanUrl,
                    ganDeviceId = cleanDevice
                )
            )
            _uiState.update {
                it.copy(
                    config = current.copy(ganApiUrl = cleanUrl, ganDeviceId = cleanDevice),
                    controlLoadedFromServer = false,
                    snackbarMessage = "Konfigurasi API disimpan ($cleanDevice)"
                )
            }
            refreshAllData(forceReloadControl = true)
        }
    }

    fun resetApiConnectionToConfigPhp() {
        saveApiConnectionConfig(
            apiUrl = GanApiService.DEFAULT_API_URL,
            deviceId = GanApiService.DEFAULT_DEVICE_ID
        )
    }

    fun testApiConnection(apiUrl: String, deviceId: String) {
        val cleanUrl = apiUrl.trim().ifBlank { GanApiService.DEFAULT_API_URL }
        val cleanDevice = deviceId.trim().ifBlank { GanApiService.DEFAULT_DEVICE_ID }
        viewModelScope.launch {
            _uiState.update { it.copy(apiTestState = GanApiTestState.Testing) }
            val res = ganApi.pingApi(cleanUrl, cleanDevice)
            _uiState.update {
                it.copy(
                    apiTestState = res.fold(
                        onSuccess = { ping -> GanApiTestState.Success(ping) },
                        onFailure = { err -> GanApiTestState.Error(err.localizedMessage ?: "Gagal terhubung") }
                    )
                )
            }
        }
    }

    fun refreshWeather(
        latitude: Double = _uiState.value.config.latitude,
        longitude: Double = _uiState.value.config.longitude,
        locationName: String = _uiState.value.config.locationName,
        silentIfCached: Boolean = false
    ) {
        viewModelScope.launch {
            val cached = when (val ws = _uiState.value.weatherState) {
                is WeatherUiState.Success -> ws.snapshot
                is WeatherUiState.Error -> ws.cached
                else -> weatherRepo.parseCachedWeather(_uiState.value.config)
            }
            if (!silentIfCached) {
                _uiState.update { it.copy(weatherState = WeatherUiState.Loading) }
            }
            val res = weatherRepo.refreshWeather(latitude, longitude, locationName)
            res.onSuccess { snap ->
                _uiState.update { it.copy(weatherState = WeatherUiState.Success(snap)) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        weatherState = WeatherUiState.Error(
                            message = err.localizedMessage ?: "Gagal memuat cuaca",
                            cached = cached
                        )
                    )
                }
            }
        }
    }

    fun selectLocationPreset(preset: LocationPreset) {
        refreshWeather(preset.latitude, preset.longitude, preset.name)
        showMessage("Lokasi cuaca diatur ke ${preset.name}")
    }

    @SuppressLint("MissingPermission")
    fun useDeviceGpsLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        val cts = CancellationTokenSource()
        _uiState.update { it.copy(weatherState = WeatherUiState.Loading) }
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    viewModelScope.launch {
                        val name = resolveCityName(loc.latitude, loc.longitude)
                        refreshWeather(loc.latitude, loc.longitude, name)
                        showMessage("Lokasi GPS: $name")
                    }
                } else {
                    val cfg = _uiState.value.config
                    refreshWeather(cfg.latitude, cfg.longitude, cfg.locationName)
                    showMessage("GPS tidak tersedia, menggunakan ${cfg.locationName}")
                }
            }
            .addOnFailureListener {
                val cfg = _uiState.value.config
                refreshWeather(cfg.latitude, cfg.longitude, cfg.locationName)
                showMessage("Gagal membaca GPS")
            }
    }

    private suspend fun resolveCityName(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(getApplication(), Locale("id", "ID"))
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            val addr = list?.firstOrNull()
            addr?.subAdminArea ?: addr?.locality ?: addr?.adminArea
                ?: String.format(Locale.US, "%.3f, %.3f", lat, lon)
        } catch (_: Exception) {
            String.format(Locale.US, "%.3f, %.3f", lat, lon)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            val current = configDao.getConfigOnce() ?: ControllerConfigEntity()
            configDao.upsertConfig(current.copy(useDynamicColor = enabled))
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }
}
