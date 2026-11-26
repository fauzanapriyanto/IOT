package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "irrigation_zones")
data class IrrigationZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val plantType: String,
    val channelNumber: Int,
    val soilMoisturePercent: Int,
    val targetMoistureMin: Int,
    val targetMoistureMax: Int,
    val flowRateLitersPerMin: Float,
    val defaultDurationMinutes: Int,
    val isValveOpen: Boolean = false,
    val remainingSeconds: Int = 0,
    val activeTotalSeconds: Int = 0,
    val activeTriggerSource: String = "Manual",
    val lastWateredAt: Long? = null,
    val autoMoistureEnabled: Boolean = true
)

@Entity(tableName = "irrigation_schedules")
data class IrrigationScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val zoneId: Int,
    val zoneName: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
    val daysMask: Int, // Bitmask: bit 0 = Sen, bit 1 = Sel, ..., bit 6 = Min
    val skipOnRain: Boolean = true,
    val isEnabled: Boolean = true
)

@Entity(tableName = "watering_logs")
data class WateringLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val zoneId: Int,
    val zoneName: String,
    val startedAt: Long,
    val durationSeconds: Int,
    val litersUsed: Float,
    val triggerSource: String, // "Manual", "Jadwal", "Otomatis", "Tunda Hujan"
    val status: String // "Selesai", "Dihentikan", "Ditunda Hujan", "Gagal Koneksi"
)

@Entity(tableName = "controller_config")
data class ControllerConfigEntity(
    @PrimaryKey val id: Int = 1,
    val ganApiUrl: String = "http://aginnusa.com:800/smart/api.php",
    val ganDeviceId: String = "GAN-IRRIGATION-001",
    val controllerUrl: String = "http://192.168.1.50",
    val apiToken: String = "",
    val isSimulationMode: Boolean = false,
    val rainSkipEnabled: Boolean = true,
    val rainThresholdPercent: Int = 60,
    val locationName: String = "Bogor, Jawa Barat",
    val latitude: Double = -6.5971,
    val longitude: Double = 106.8060,
    val useDynamicColor: Boolean = false,
    val cachedWeatherJson: String? = null,
    val lastWeatherSyncAt: Long? = null
)
