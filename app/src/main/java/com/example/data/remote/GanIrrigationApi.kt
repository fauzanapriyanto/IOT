package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GanSensorReadingDto(
    val id: Long? = null,
    @Json(name = "device_id") val deviceId: String = "GAN-IRRIGATION-001",
    @Json(name = "water_level") val waterLevel: Double = 0.0,
    @Json(name = "soil_moisture") val soilMoisture: Double = 0.0,
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    @Json(name = "raw_water") val rawWater: Int? = null,
    @Json(name = "raw_soil") val rawSoil: Int? = null,
    @Json(name = "water_sensor_status") val waterSensorStatus: String? = null,
    @Json(name = "soil_sensor_status") val soilSensorStatus: String? = null,
    @Json(name = "dht_sensor_status") val dhtSensorStatus: String? = null,
    @Json(name = "water_pump") val waterPump: Int = 0,
    @Json(name = "soil_pump") val soilPump: Int = 0,
    @Json(name = "water_mode") val waterMode: String? = null,
    @Json(name = "soil_mode") val soilMode: String? = null,
    @Json(name = "sensor_status") val sensorStatus: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GanControlConfigDto(
    @Json(name = "device_id") val deviceId: String = "GAN-IRRIGATION-001",
    @Json(name = "water_mode") val waterMode: String = "AUTO",
    @Json(name = "soil_mode") val soilMode: String = "AUTO",
    @Json(name = "water_pump") val waterPump: Int = 0,
    @Json(name = "soil_pump") val soilPump: Int = 0,
    @Json(name = "timer_enabled") val timerEnabled: Boolean = false,
    @Json(name = "timer_start") val timerStart: String = "06:00",
    @Json(name = "timer_end") val timerEnd: String = "06:30",
    @Json(name = "water_on_at") val waterOnAt: Double = 50.0,
    @Json(name = "water_off_at") val waterOffAt: Double = 80.0,
    @Json(name = "soil_on_at") val soilOnAt: Double = 70.0,
    @Json(name = "soil_off_at") val soilOffAt: Double = 85.0
)

data class GanSummaryData(
    val totalData: Int = 0,
    val avgWater: Double = 0.0,
    val avgSoil: Double = 0.0,
    val avgTemperature: Double = 0.0,
    val avgHumidity: Double = 0.0,
    val lastUpdate: String? = null
)

data class GanInsertPayload(
    val deviceId: String,
    val waterLevel: Double,
    val soilMoisture: Double,
    val temperature: Double,
    val humidity: Double,
    val rawWater: Int = 0,
    val rawSoil: Int = 0,
    val waterSensorStatus: String = "OK",
    val soilSensorStatus: String = "OK",
    val dhtSensorStatus: String = "OK",
    val waterPump: Int = 0,
    val soilPump: Int = 0,
    val waterMode: String = "AUTO",
    val soilMode: String = "AUTO"
)

data class GanApiPingResult(
    val httpCode: Int,
    val latencyMs: Long,
    val message: String
)

class GanApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchLatest(apiUrl: String, deviceId: String): Result<GanSensorReadingDto?> =
        withContext(Dispatchers.IO) {
            try {
                val url = buildQueryUrl(apiUrl, "latest", deviceId)
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val raw = cleanJson(resp.body?.string().orEmpty())
                    val json = parseJsonObject(raw, resp.code)
                    if (!json.optBoolean("success", false)) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("message", "API latest gagal"))
                        )
                    }
                    val dataObj = json.optJSONObject("data")
                    if (dataObj == null || dataObj.length() == 0) {
                        return@withContext Result.success(null)
                    }
                    Result.success(parseReading(dataObj))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchHistory(
        apiUrl: String,
        deviceId: String,
        limit: Int = 100
    ): Result<List<GanSensorReadingDto>> = withContext(Dispatchers.IO) {
        try {
            val encodedDevice = URLEncoder.encode(deviceId, "UTF-8")
            val sep = if (apiUrl.contains("?")) "&" else "?"
            val url = "${apiUrl.trim()}${sep}action=history&device_id=$encodedDevice&limit=$limit&_=${System.currentTimeMillis()}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val raw = cleanJson(resp.body?.string().orEmpty())
                val json = parseJsonObject(raw, resp.code)
                if (!json.optBoolean("success", false)) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("message", "API history gagal"))
                    )
                }
                val arr = json.optJSONArray("data")
                val list = mutableListOf<GanSensorReadingDto>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        list.add(parseReading(obj))
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSummary(apiUrl: String, deviceId: String): Result<GanSummaryData> =
        withContext(Dispatchers.IO) {
            try {
                val url = buildQueryUrl(apiUrl, "summary", deviceId)
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val raw = cleanJson(resp.body?.string().orEmpty())
                    val json = parseJsonObject(raw, resp.code)
                    if (!json.optBoolean("success", false)) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("message", "API summary gagal"))
                        )
                    }
                    val d = json.optJSONObject("data") ?: JSONObject()
                    Result.success(
                        GanSummaryData(
                            totalData = d.optInt("total_data", 0),
                            avgWater = d.optDouble("avg_water", 0.0),
                            avgSoil = d.optDouble("avg_soil", 0.0),
                            avgTemperature = d.optDouble("avg_temperature", 0.0),
                            avgHumidity = d.optDouble("avg_humidity", 0.0),
                            lastUpdate = d.optString("last_update").takeIf { it.isNotBlank() && it != "null" }
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchControl(apiUrl: String, deviceId: String): Result<GanControlConfigDto> =
        withContext(Dispatchers.IO) {
            try {
                val url = buildQueryUrl(apiUrl, "control", deviceId)
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val raw = cleanJson(resp.body?.string().orEmpty())
                    val json = parseJsonObject(raw, resp.code)
                    // Note: api.php ?action=control returns raw object directly, or error wrapper on failure
                    if (json.has("success") && !json.optBoolean("success", true)) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("message", "API control gagal"))
                        )
                    }
                    val timerEnabledRaw = json.opt("timer_enabled")
                    val timerEnabled = when (timerEnabledRaw) {
                        is Boolean -> timerEnabledRaw
                        is Number -> timerEnabledRaw.toInt() == 1
                        is String -> timerEnabledRaw == "1" || timerEnabledRaw.equals("true", true)
                        else -> false
                    }
                    Result.success(
                        GanControlConfigDto(
                            deviceId = json.optString("device_id", deviceId),
                            waterMode = json.optString("water_mode", "AUTO"),
                            soilMode = json.optString("soil_mode", "AUTO"),
                            waterPump = json.optInt("water_pump", 0),
                            soilPump = json.optInt("soil_pump", 0),
                            timerEnabled = timerEnabled,
                            timerStart = json.optString("timer_start", "06:00").take(5),
                            timerEnd = json.optString("timer_end", "06:30").take(5),
                            waterOnAt = json.optDouble("water_on_at", 50.0),
                            waterOffAt = json.optDouble("water_off_at", 80.0),
                            soilOnAt = json.optDouble("soil_on_at", 70.0),
                            soilOffAt = json.optDouble("soil_off_at", 85.0)
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateControl(apiUrl: String, control: GanControlConfigDto): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "update_control")
                    put("device_id", control.deviceId)
                    put("water_mode", control.waterMode)
                    put("soil_mode", control.soilMode)
                    put("water_pump", control.waterPump)
                    put("soil_pump", control.soilPump)
                    put("timer_enabled", if (control.timerEnabled) 1 else 0)
                    put("timer_start", control.timerStart.take(5))
                    put("timer_end", control.timerEnd.take(5))
                    put("water_on_at", control.waterOnAt)
                    put("water_off_at", control.waterOffAt)
                    put("soil_on_at", control.soilOnAt)
                    put("soil_off_at", control.soilOffAt)
                }.toString()

                val req = Request.Builder()
                    .url(apiUrl.trim())
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val raw = cleanJson(resp.body?.string().orEmpty())
                    val json = parseJsonObject(raw, resp.code)
                    if (!json.optBoolean("success", false)) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("message", "Gagal menyimpan kontrol"))
                        )
                    }
                    Result.success(json.optString("message", "Pengaturan kontrol tersimpan"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun insertSensorData(apiUrl: String, payloadData: GanInsertPayload): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "insert")
                    put("device_id", payloadData.deviceId)
                    put("water_level", payloadData.waterLevel)
                    put("soil_moisture", payloadData.soilMoisture)
                    put("temperature", payloadData.temperature)
                    put("humidity", payloadData.humidity)
                    put("raw_water", payloadData.rawWater)
                    put("raw_soil", payloadData.rawSoil)
                    put("water_sensor_status", payloadData.waterSensorStatus)
                    put("soil_sensor_status", payloadData.soilSensorStatus)
                    put("dht_sensor_status", payloadData.dhtSensorStatus)
                    put("water_pump", payloadData.waterPump)
                    put("soil_pump", payloadData.soilPump)
                    put("water_mode", payloadData.waterMode)
                    put("soil_mode", payloadData.soilMode)
                }.toString()

                val req = Request.Builder()
                    .url(apiUrl.trim())
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val raw = cleanJson(resp.body?.string().orEmpty())
                    val json = parseJsonObject(raw, resp.code)
                    if (!json.optBoolean("success", false)) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("message", "Gagal mengirim data sensor"))
                        )
                    }
                    Result.success(json.optString("message", "Data sensor berhasil dikirim"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun pingApi(apiUrl: String, deviceId: String): Result<GanApiPingResult> =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val url = buildQueryUrl(apiUrl, "latest", deviceId)
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val elapsed = System.currentTimeMillis() - start
                    val raw = cleanJson(resp.body?.string().orEmpty())
                    val json = parseJsonObject(raw, resp.code)
                    val ok = json.optBoolean("success", false)
                    val msg = json.optString("message", "OK")
                    if (resp.isSuccessful && ok) {
                        Result.success(GanApiPingResult(resp.code, elapsed, "Terhubung ke api.php: $msg"))
                    } else {
                        Result.failure(IllegalStateException("HTTP ${resp.code}: $msg"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException("Gagal koneksi ke api.php: ${e.localizedMessage ?: "Periksa URL/internet"}")
                )
            }
        }

    private fun buildQueryUrl(apiUrl: String, action: String, deviceId: String): String {
        val clean = apiUrl.trim()
        val sep = if (clean.contains("?")) "&" else "?"
        val encodedDevice = URLEncoder.encode(deviceId, "UTF-8")
        return "$clean${sep}action=$action&device_id=$encodedDevice&_=${System.currentTimeMillis()}"
    }

    private fun cleanJson(raw: String): String = raw.replace("\uFEFF", "").trim()

    private fun parseJsonObject(raw: String, httpCode: Int): JSONObject {
        if (raw.isBlank()) {
            throw IllegalStateException("Respons kosong dari server (HTTP $httpCode)")
        }
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            val preview = raw.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim().take(160)
            throw IllegalStateException("Respons bukan JSON (HTTP $httpCode): $preview")
        }
    }

    private fun parseReading(obj: JSONObject): GanSensorReadingDto {
        return GanSensorReadingDto(
            id = obj.optLong("id", 0L).takeIf { it > 0L },
            deviceId = obj.optString("device_id", "GAN-IRRIGATION-001"),
            waterLevel = obj.optDouble("water_level", 0.0),
            soilMoisture = obj.optDouble("soil_moisture", 0.0),
            temperature = obj.optDouble("temperature", 0.0),
            humidity = obj.optDouble("humidity", 0.0),
            rawWater = if (obj.has("raw_water")) obj.optInt("raw_water") else null,
            rawSoil = if (obj.has("raw_soil")) obj.optInt("raw_soil") else null,
            waterSensorStatus = obj.optString("water_sensor_status").takeIf { it.isNotBlank() && it != "null" },
            soilSensorStatus = obj.optString("soil_sensor_status").takeIf { it.isNotBlank() && it != "null" },
            dhtSensorStatus = obj.optString("dht_sensor_status").takeIf { it.isNotBlank() && it != "null" },
            waterPump = obj.optInt("water_pump", 0),
            soilPump = obj.optInt("soil_pump", 0),
            waterMode = obj.optString("water_mode").takeIf { it.isNotBlank() && it != "null" },
            soilMode = obj.optString("soil_mode").takeIf { it.isNotBlank() && it != "null" },
            sensorStatus = obj.optString("sensor_status").takeIf { it.isNotBlank() && it != "null" },
            createdAt = obj.optString("created_at").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    companion object {
        const val DEFAULT_API_URL = "http://aginnusa.com:800/smart/api.php"
        const val DEFAULT_DEVICE_ID = "GAN-IRRIGATION-001"
    }
}
