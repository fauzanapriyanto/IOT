package com.example.data.repository

import com.example.data.local.ControllerConfigDao
import com.example.data.local.ControllerConfigEntity
import com.example.data.local.IrrigationScheduleEntity
import com.example.data.local.IrrigationZoneEntity
import com.example.data.local.ScheduleDao
import com.example.data.local.WateringLogDao
import com.example.data.local.WateringLogEntity
import com.example.data.local.ZoneDao
import com.example.data.remote.ControllerPingResult
import com.example.data.remote.IoTControllerClient
import com.example.data.remote.OpenMeteoResponse
import com.example.data.remote.OpenMeteoService
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min

data class DailyForecastItem(
    val date: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationSumMm: Double,
    val rainProbabilityMax: Int,
    val et0Mm: Double
)

data class WeatherSnapshot(
    val locationName: String,
    val temperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val currentPrecipitationMm: Double,
    val weatherCode: Int,
    val nextHoursRainProbability: Int,
    val todayEt0Mm: Double,
    val forecastDays: List<DailyForecastItem>,
    val fetchedAtMillis: Long
)

class IrrigationRepository(
    private val zoneDao: ZoneDao,
    private val scheduleDao: ScheduleDao,
    private val logDao: WateringLogDao,
    private val configDao: ControllerConfigDao,
    private val weatherService: OpenMeteoService,
    private val iotClient: IoTControllerClient
) {
    private val moshi = Moshi.Builder().build()
    private val weatherAdapter = moshi.adapter(OpenMeteoResponse::class.java)

    val zonesFlow: Flow<List<IrrigationZoneEntity>> = zoneDao.observeAllZones()
    val schedulesFlow: Flow<List<IrrigationScheduleEntity>> = scheduleDao.observeAllSchedules()
    val logsFlow: Flow<List<WateringLogEntity>> = logDao.observeRecentLogs()
    val configFlow: Flow<ControllerConfigEntity> = configDao.observeConfig().map {
        it ?: ControllerConfigEntity()
    }

    suspend fun ensureSeeded() {
        if (configDao.getConfigOnce() == null) {
            configDao.upsertConfig(ControllerConfigEntity())
        }
        val existingZones = zoneDao.getAllZonesOnce()
        if (existingZones.isEmpty()) {
            val now = System.currentTimeMillis()
            val z1Id = zoneDao.insertZone(
                IrrigationZoneEntity(
                    name = "Taman Depan & Rumput",
                    plantType = "Rumput Zoysia & Semak Hias",
                    channelNumber = 1,
                    soilMoisturePercent = 42,
                    targetMoistureMin = 45,
                    targetMoistureMax = 75,
                    flowRateLitersPerMin = 12.5f,
                    defaultDurationMinutes = 10,
                    lastWateredAt = now - 8 * 3600_000L,
                    autoMoistureEnabled = true
                )
            ).toInt()
            val z2Id = zoneDao.insertZone(
                IrrigationZoneEntity(
                    name = "Kebun Sayur Organik",
                    plantType = "Cabai, Tomat & Selada",
                    channelNumber = 2,
                    soilMoisturePercent = 34,
                    targetMoistureMin = 50,
                    targetMoistureMax = 80,
                    flowRateLitersPerMin = 8.0f,
                    defaultDurationMinutes = 12,
                    lastWateredAt = now - 14 * 3600_000L,
                    autoMoistureEnabled = true
                )
            ).toInt()
            val z3Id = zoneDao.insertZone(
                IrrigationZoneEntity(
                    name = "Greenhouse Anggrek",
                    plantType = "Anggrek & Tanaman Tropis",
                    channelNumber = 3,
                    soilMoisturePercent = 68,
                    targetMoistureMin = 60,
                    targetMoistureMax = 85,
                    flowRateLitersPerMin = 4.5f,
                    defaultDurationMinutes = 6,
                    lastWateredAt = now - 4 * 3600_000L,
                    autoMoistureEnabled = true
                )
            ).toInt()
            val z4Id = zoneDao.insertZone(
                IrrigationZoneEntity(
                    name = "Halaman Belakang",
                    plantType = "Pohon Buah & Tabulampot",
                    channelNumber = 4,
                    soilMoisturePercent = 56,
                    targetMoistureMin = 40,
                    targetMoistureMax = 70,
                    flowRateLitersPerMin = 15.0f,
                    defaultDurationMinutes = 15,
                    lastWateredAt = now - 20 * 3600_000L,
                    autoMoistureEnabled = false
                )
            ).toInt()

            scheduleDao.insertSchedules(
                listOf(
                    IrrigationScheduleEntity(
                        zoneId = z1Id,
                        zoneName = "Taman Depan & Rumput",
                        label = "Siram Pagi Rutin",
                        hour = 6,
                        minute = 0,
                        durationMinutes = 10,
                        daysMask = 0b1111111,
                        skipOnRain = true,
                        isEnabled = true
                    ),
                    IrrigationScheduleEntity(
                        zoneId = z2Id,
                        zoneName = "Kebun Sayur Organik",
                        label = "Hidrasi Sayur Pagi & Sore",
                        hour = 16,
                        minute = 30,
                        durationMinutes = 12,
                        daysMask = 0b1111111,
                        skipOnRain = true,
                        isEnabled = true
                    ),
                    IrrigationScheduleEntity(
                        zoneId = z3Id,
                        zoneName = "Greenhouse Anggrek",
                        label = "Kabut Embun Pagi",
                        hour = 7,
                        minute = 15,
                        durationMinutes = 6,
                        daysMask = 0b1010101,
                        skipOnRain = false,
                        isEnabled = true
                    )
                )
            )

            logDao.insertLogs(
                listOf(
                    WateringLogEntity(
                        zoneId = z3Id,
                        zoneName = "Greenhouse Anggrek",
                        startedAt = now - 4 * 3600_000L,
                        durationSeconds = 360,
                        litersUsed = 27.0f,
                        triggerSource = "Jadwal",
                        status = "Selesai"
                    ),
                    WateringLogEntity(
                        zoneId = z1Id,
                        zoneName = "Taman Depan & Rumput",
                        startedAt = now - 8 * 3600_000L,
                        durationSeconds = 600,
                        litersUsed = 125.0f,
                        triggerSource = "Jadwal",
                        status = "Selesai"
                    ),
                    WateringLogEntity(
                        zoneId = z2Id,
                        zoneName = "Kebun Sayur Organik",
                        startedAt = now - 26 * 3600_000L,
                        durationSeconds = 720,
                        litersUsed = 96.0f,
                        triggerSource = "Manual",
                        status = "Selesai"
                    ),
                    WateringLogEntity(
                        zoneId = z4Id,
                        zoneName = "Halaman Belakang",
                        startedAt = now - 50 * 3600_000L,
                        durationSeconds = 900,
                        litersUsed = 225.0f,
                        triggerSource = "Jadwal",
                        status = "Selesai"
                    )
                )
            )
        }
    }

    fun parseCachedWeather(config: ControllerConfigEntity): WeatherSnapshot? {
        val raw = config.cachedWeatherJson ?: return null
        return try {
            val dto = weatherAdapter.fromJson(raw) ?: return null
            mapDtoToSnapshot(dto, config.locationName, config.lastWeatherSyncAt ?: System.currentTimeMillis())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshWeather(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): Result<WeatherSnapshot> {
        return try {
            val response = weatherService.getForecast(latitude = latitude, longitude = longitude)
            val now = System.currentTimeMillis()
            val json = weatherAdapter.toJson(response)
            val currentCfg = configDao.getConfigOnce() ?: ControllerConfigEntity()
            configDao.upsertConfig(
                currentCfg.copy(
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName,
                    cachedWeatherJson = json,
                    lastWeatherSyncAt = now
                )
            )
            Result.success(mapDtoToSnapshot(response, locationName, now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDtoToSnapshot(
        dto: OpenMeteoResponse,
        locationName: String,
        fetchedAt: Long
    ): WeatherSnapshot {
        val current = dto.current
        val hourlyProbs = dto.hourly?.precipitationProbability?.take(12).orEmpty()
        val maxUpcomingRainProb = hourlyProbs.maxOrNull() ?: 0

        val daily = dto.daily
        val daysCount = daily?.time?.size ?: 0
        val forecastItems = (0 until daysCount).map { i ->
            DailyForecastItem(
                date = daily?.time?.getOrNull(i).orEmpty(),
                weatherCode = daily?.weatherCode?.getOrNull(i) ?: 0,
                tempMax = daily?.temperature2mMax?.getOrNull(i) ?: 0.0,
                tempMin = daily?.temperature2mMin?.getOrNull(i) ?: 0.0,
                precipitationSumMm = daily?.precipitationSum?.getOrNull(i) ?: 0.0,
                rainProbabilityMax = daily?.precipitationProbabilityMax?.getOrNull(i) ?: 0,
                et0Mm = daily?.et0FaoEvapotranspiration?.getOrNull(i) ?: 0.0
            )
        }

        return WeatherSnapshot(
            locationName = locationName,
            temperatureC = current?.temperature2m ?: 0.0,
            humidityPercent = current?.relativeHumidity2m ?: 0,
            windSpeedKmh = current?.windSpeed10m ?: 0.0,
            currentPrecipitationMm = current?.precipitation ?: 0.0,
            weatherCode = current?.weatherCode ?: 0,
            nextHoursRainProbability = maxUpcomingRainProb,
            todayEt0Mm = forecastItems.firstOrNull()?.et0Mm ?: 0.0,
            forecastDays = forecastItems,
            fetchedAtMillis = fetchedAt
        )
    }

    suspend fun startZoneWatering(
        zone: IrrigationZoneEntity,
        durationMinutes: Int,
        triggerSource: String
    ): Result<String> {
        val cfg = configDao.getConfigOnce() ?: ControllerConfigEntity()
        val clampedDuration = durationMinutes.coerceIn(1, 120)

        if (!cfg.isSimulationMode) {
            val remoteResult = iotClient.sendValveCommand(
                baseUrl = cfg.controllerUrl,
                apiToken = cfg.apiToken,
                channel = zone.channelNumber,
                open = true,
                durationMinutes = clampedDuration
            )
            if (remoteResult.isFailure) {
                logDao.insertLog(
                    WateringLogEntity(
                        zoneId = zone.id,
                        zoneName = zone.name,
                        startedAt = System.currentTimeMillis(),
                        durationSeconds = 0,
                        litersUsed = 0f,
                        triggerSource = triggerSource,
                        status = "Gagal Koneksi"
                    )
                )
                return remoteResult
            }
        }

        val totalSec = clampedDuration * 60
        zoneDao.updateZone(
            zone.copy(
                isValveOpen = true,
                remainingSeconds = totalSec,
                activeTotalSeconds = totalSec,
                activeTriggerSource = triggerSource
            )
        )
        return Result.success(
            if (cfg.isSimulationMode) {
                "Simulasi: Katup ${zone.name} dibuka ($clampedDuration menit)"
            } else {
                "Katup ${zone.name} (CH ${zone.channelNumber}) aktif ($clampedDuration menit)"
            }
        )
    }

    suspend fun stopZoneWatering(
        zone: IrrigationZoneEntity,
        completedNaturally: Boolean = false
    ): Result<String> {
        val cfg = configDao.getConfigOnce() ?: ControllerConfigEntity()
        if (!cfg.isSimulationMode) {
            iotClient.sendValveCommand(
                baseUrl = cfg.controllerUrl,
                apiToken = cfg.apiToken,
                channel = zone.channelNumber,
                open = false,
                durationMinutes = 0
            )
        }

        val elapsedSeconds = max(1, zone.activeTotalSeconds - zone.remainingSeconds)
        val liters = (elapsedSeconds / 60f) * zone.flowRateLitersPerMin
        val now = System.currentTimeMillis()

        zoneDao.updateZone(
            zone.copy(
                isValveOpen = false,
                remainingSeconds = 0,
                activeTotalSeconds = 0,
                lastWateredAt = now
            )
        )

        logDao.insertLog(
            WateringLogEntity(
                zoneId = zone.id,
                zoneName = zone.name,
                startedAt = now - elapsedSeconds * 1000L,
                durationSeconds = elapsedSeconds,
                litersUsed = liters,
                triggerSource = zone.activeTriggerSource,
                status = if (completedNaturally) "Selesai" else "Dihentikan"
            )
        )
        return Result.success("Katup ${zone.name} ditutup (${String.format("%.1f", liters)} L)")
    }

    suspend fun tickActiveZonesSimulationAndCountdown() {
        val zones = zoneDao.getAllZonesOnce()
        val cfg = configDao.getConfigOnce() ?: ControllerConfigEntity()
        for (zone in zones) {
            if (!zone.isValveOpen) continue
            val nextRemaining = zone.remainingSeconds - 1
            val moistureGain = if (cfg.isSimulationMode && zone.remainingSeconds % 8 == 0) 1 else 0
            val nextMoisture = min(98, zone.soilMoisturePercent + moistureGain)
            if (nextRemaining <= 0) {
                val updatedZone = zone.copy(
                    soilMoisturePercent = nextMoisture,
                    remainingSeconds = 0
                )
                stopZoneWatering(updatedZone, completedNaturally = true)
            } else {
                zoneDao.updateZone(
                    zone.copy(
                        remainingSeconds = nextRemaining,
                        soilMoisturePercent = nextMoisture
                    )
                )
            }
        }
    }

    suspend fun stopAllZones(): Int {
        val zones = zoneDao.getAllZonesOnce().filter { it.isValveOpen }
        zones.forEach { stopZoneWatering(it, completedNaturally = false) }
        return zones.size
    }

    suspend fun startAllZones(durationMinutes: Int): Result<String> {
        val zones = zoneDao.getAllZonesOnce()
        if (zones.isEmpty()) return Result.failure(IllegalStateException("Belum ada zona irigasi."))
        var started = 0
        var lastError: Throwable? = null
        for (zone in zones) {
            if (!zone.isValveOpen) {
                val res = startZoneWatering(zone, durationMinutes, "Manual")
                if (res.isSuccess) started++ else lastError = res.exceptionOrNull()
            }
        }
        return if (started > 0) {
            Result.success("$started zona mulai menyiram ($durationMinutes menit)")
        } else {
            Result.failure(lastError ?: IllegalStateException("Semua zona sudah aktif"))
        }
    }

    suspend fun evaluateSmartAutomation(weather: WeatherSnapshot?): String {
        val cfg = configDao.getConfigOnce() ?: ControllerConfigEntity()
        val zones = zoneDao.getAllZonesOnce()
        val schedules = scheduleDao.getAllSchedulesOnce().filter { it.isEnabled }
        val rainProb = weather?.nextHoursRainProbability ?: 0

        if (cfg.rainSkipEnabled && weather != null && rainProb >= cfg.rainThresholdPercent) {
            val activeRainSensitive = schedules.filter { it.skipOnRain }
            activeRainSensitive.take(1).forEach { sched ->
                logDao.insertLog(
                    WateringLogEntity(
                        zoneId = sched.zoneId,
                        zoneName = sched.zoneName,
                        startedAt = System.currentTimeMillis(),
                        durationSeconds = 0,
                        litersUsed = 0f,
                        triggerSource = "Tunda Hujan",
                        status = "Ditunda Hujan ($rainProb%)"
                    )
                )
            }
            return "Tunda Hujan aktif: peluang hujan $rainProb% (>= ${cfg.rainThresholdPercent}%). Penyiraman otomatis ditangguhkan."
        }

        val dryZones = zones.filter {
            it.autoMoistureEnabled && !it.isValveOpen && it.soilMoisturePercent < it.targetMoistureMin
        }
        if (dryZones.isEmpty()) {
            return "Semua zona dalam batas kelembapan optimal. Tidak ada penyiraman darurat dibutuhkan."
        }

        var activated = 0
        dryZones.forEach { z ->
            val res = startZoneWatering(z, z.defaultDurationMinutes, "Otomatis")
            if (res.isSuccess) activated++
        }
        return if (activated > 0) {
            "Otomatis menyalakan $activated zona kering di bawah ambang kelembapan."
        } else {
            "Gagal menyalakan zona otomatis. Periksa koneksi kontroler."
        }
    }

    suspend fun saveZone(zone: IrrigationZoneEntity) {
        zoneDao.insertZone(zone)
    }

    suspend fun deleteZone(zone: IrrigationZoneEntity) {
        scheduleDao.deleteByZoneId(zone.id)
        zoneDao.deleteZone(zone)
    }

    suspend fun adjustSimulationMoisture(zone: IrrigationZoneEntity, delta: Int) {
        val next = (zone.soilMoisturePercent + delta).coerceIn(5, 99)
        zoneDao.updateZone(zone.copy(soilMoisturePercent = next))
    }

    suspend fun saveSchedule(schedule: IrrigationScheduleEntity) {
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun toggleSchedule(schedule: IrrigationScheduleEntity, enabled: Boolean) {
        scheduleDao.updateSchedule(schedule.copy(isEnabled = enabled))
    }

    suspend fun deleteSchedule(schedule: IrrigationScheduleEntity) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun updateConfig(transform: (ControllerConfigEntity) -> ControllerConfigEntity) {
        val current = configDao.getConfigOnce() ?: ControllerConfigEntity()
        configDao.upsertConfig(transform(current))
    }

    suspend fun testHardwareConnection(url: String, token: String): Result<ControllerPingResult> {
        return iotClient.testConnection(url, token)
    }

    suspend fun clearLogs() {
        logDao.clearAllLogs()
    }
}
