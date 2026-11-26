package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ControllerPingResult(
    val reachable: Boolean,
    val statusCode: Int,
    val message: String,
    val latencyMs: Long
)

class IoTControllerClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(baseUrl: String, apiToken: String): Result<ControllerPingResult> =
        withContext(Dispatchers.IO) {
            val cleanBase = normalizeUrl(baseUrl)
            if (cleanBase.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL kontroler belum diisi."))
            }
            val startMs = System.currentTimeMillis()
            try {
                val reqBuilder = Request.Builder()
                    .url("$cleanBase/status")
                    .get()
                if (apiToken.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${apiToken.trim()}")
                }
                client.newCall(reqBuilder.build()).execute().use { response ->
                    val elapsed = System.currentTimeMillis() - startMs
                    val bodyPreview = response.body?.string()?.take(140).orEmpty()
                    if (response.isSuccessful) {
                        Result.success(
                            ControllerPingResult(
                                reachable = true,
                                statusCode = response.code,
                                message = if (bodyPreview.isNotBlank()) "Terhubung (${response.code}): $bodyPreview" else "Terhubung ke kontroler (${response.code})",
                                latencyMs = elapsed
                            )
                        )
                    } else {
                        Result.failure(
                            IllegalStateException("Kontroler merespons HTTP ${response.code} (${elapsed} ms)")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException("Gagal terhubung ke $cleanBase: ${e.localizedMessage ?: "Periksa IP/Wi-Fi atau aktifkan Mode Simulasi"}")
                )
            }
        }

    suspend fun sendValveCommand(
        baseUrl: String,
        apiToken: String,
        channel: Int,
        open: Boolean,
        durationMinutes: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanBase = normalizeUrl(baseUrl)
        if (cleanBase.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("URL kontroler belum dikonfigurasi."))
        }
        try {
            val payload = JSONObject().apply {
                put("channel", channel)
                put("state", if (open) "ON" else "OFF")
                put("open", open)
                put("duration_minutes", if (open) durationMinutes else 0)
            }.toString()

            val reqBuilder = Request.Builder()
                .url("$cleanBase/api/zones/$channel/control")
                .post(payload.toRequestBody(jsonMediaType))
            if (apiToken.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${apiToken.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Perintah katup CH $channel (${if (open) "BUKA" else "TUTUP"}) diterima perangkat.")
                } else {
                    Result.failure(IllegalStateException("Kontroler menolak perintah: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Gagal mengirim perintah ke CH $channel: ${e.localizedMessage ?: "Tidak dapat menjangkau kontroler"}")
            )
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }
}
