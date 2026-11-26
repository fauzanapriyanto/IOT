package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.remote.GanSensorReadingDto
import com.example.ui.components.AnimatedWaterTankGauge
import com.example.ui.components.ChartSeriesSpec
import com.example.ui.components.DualLineSensorChartCard
import com.example.ui.components.GanServerStatusBanner
import com.example.ui.components.toneColor
import com.example.ui.state.GanUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureOptimalGreen
import java.util.Locale

@Composable
fun GanDashboardScreen(
    uiState: GanUiState,
    onRefreshAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latest = uiState.latestReading
    val chronologicalHistory = uiState.historyRows.take(30).reversed()
    val chartTimeLabels = chronologicalHistory.map { row ->
        val raw = row.createdAt.orEmpty()
        if (raw.length >= 16) raw.substring(11, 16) else raw
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GanHeroCard(
                deviceId = uiState.config.ganDeviceId,
                latest = latest,
                isServerOnline = uiState.isServerOnline
            )
        }

        item {
            GanServerStatusBanner(
                deviceId = uiState.config.ganDeviceId,
                apiUrl = uiState.config.ganApiUrl,
                isOnline = uiState.isServerOnline,
                lastUpdate = latest?.createdAt,
                onRefresh = onRefreshAll
            )
        }

        if (!uiState.lastErrorMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.lastErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Row 1: Water Level Tank & Soil Moisture
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaterTankSensorCard(
                    waterLevel = latest?.waterLevel,
                    modifier = Modifier.weight(1f)
                )
                SoilMoistureSensorCard(
                    soilMoisture = latest?.soilMoisture,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Temperature & Humidity
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TemperatureSensorCard(
                    temperature = latest?.temperature,
                    modifier = Modifier.weight(1f)
                )
                HumiditySensorCard(
                    humidity = latest?.humidity,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 24h Summary Card from ?action=summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan 24 Jam (${uiState.summary.totalData} Data)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStatBox("Avg Water", "${fmt(uiState.summary.avgWater)}%", Modifier.weight(1f))
                        SummaryStatBox("Avg Soil", "${fmt(uiState.summary.avgSoil)}%", Modifier.weight(1f))
                        SummaryStatBox("Avg Suhu", "${fmt(uiState.summary.avgTemperature)}°C", Modifier.weight(1f))
                        SummaryStatBox("Avg Lembap", "${fmt(uiState.summary.avgHumidity)}%", Modifier.weight(1f))
                    }
                }
            }
        }

        // Chart 1: Water Level & Soil Moisture
        item {
            DualLineSensorChartCard(
                title = "Grafik Water Level & Soil Moisture",
                seriesA = ChartSeriesSpec(
                    label = "Water Level (%)",
                    color = Color(0xFF0284C7),
                    values = chronologicalHistory.map { it.waterLevel.toFloat() },
                    minRange = 0f,
                    maxRange = 100f
                ),
                seriesB = ChartSeriesSpec(
                    label = "Soil Moisture (%)",
                    color = Color(0xFF16A34A),
                    values = chronologicalHistory.map { it.soilMoisture.toFloat() },
                    minRange = 0f,
                    maxRange = 100f
                ),
                timeLabels = chartTimeLabels
            )
        }

        // Chart 2: Temperature & Humidity
        item {
            DualLineSensorChartCard(
                title = "Grafik Temperature & Humidity",
                seriesA = ChartSeriesSpec(
                    label = "Temperature (°C)",
                    color = Color(0xFFEA580C),
                    values = chronologicalHistory.map { it.temperature.toFloat() },
                    minRange = 0f,
                    maxRange = 50f
                ),
                seriesB = ChartSeriesSpec(
                    label = "Humidity (%)",
                    color = Color(0xFF7C3AED),
                    values = chronologicalHistory.map { it.humidity.toFloat() },
                    minRange = 0f,
                    maxRange = 100f
                ),
                timeLabels = chartTimeLabels
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun GanHeroCard(
    deviceId: String,
    latest: GanSensorReadingDto?,
    isServerOnline: Boolean
) {
    val waterPumpOn = (latest?.waterPump ?: 0) == 1
    val soilPumpOn = (latest?.soilPump ?: 0) == 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_garden),
            contentDescription = "Latar GAN Smart Irrigation",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF052E24).copy(alpha = 0.45f),
                            Color(0xFF052E24).copy(alpha = 0.90f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GAN SMART IRRIGATION IoT",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PT GOLDEN AGIN NUSA • $deviceId",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isServerOnline) MoistureOptimalGreen else Color(0xFFDC2626)
                ) {
                    Text(
                        text = if (isServerOnline) "ONLINE" else "OFFLINE",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroPumpBadge(
                    label = "WATER PUMP",
                    isOn = waterPumpOn,
                    modifier = Modifier.weight(1f)
                )
                HeroPumpBadge(
                    label = "SOIL PUMP",
                    isOn = soilPumpOn,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroPumpBadge(label: String, isOn: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOn) Color(0xFF16A34A) else Color(0xFF475569)
            ) {
                Text(
                    text = if (isOn) "ON" else "OFF",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun WaterTankSensorCard(
    waterLevel: Double?,
    modifier: Modifier = Modifier
) {
    val water = (waterLevel ?: 0.0).coerceIn(0.0, 100.0)
    val (tone, label) = when {
        waterLevel == null -> "blue" to "MEMUAT..."
        water <= 25.0 -> "red" to "AIR RENDAH"
        water <= 50.0 -> "yellow" to "AIR SEDANG"
        water < 95.0 -> "green" to "AIR CUKUP"
        else -> "blue" to "TANGKI PENUH"
    }
    val accent = toneColor(tone)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WATER LEVEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Filled.Water, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            AnimatedWaterTankGauge(
                waterPercent = if (waterLevel != null) water else 0.0,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (waterLevel != null) "${fmt(water)}%" else "--%",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadgePill(label = label, color = accent)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ketinggian air tandon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SoilMoistureSensorCard(
    soilMoisture: Double?,
    modifier: Modifier = Modifier
) {
    val soil = (soilMoisture ?: 0.0).coerceIn(0.0, 100.0)
    val (tone, label, desc, icon) = when {
        soilMoisture == null -> Quad("green", "MEMUAT...", "Kondisi kelembaban tanah", Icons.Filled.Eco)
        soil < 30.0 -> Quad("red", "KERING", "Tanah kering — butuh air", Icons.Filled.WbSunny)
        soil <= 70.0 -> Quad("green", "LEMBAB", "Kelembaban tanah terjaga", Icons.Filled.Eco)
        else -> Quad("blue", "BASAH", "Tanah sangat basah", Icons.Filled.WaterDrop)
    }
    val accent = toneColor(tone)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOIL MOISTURE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(46.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (soilMoisture != null) "${fmt(soil)}%" else "--%",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadgePill(label = label, color = accent)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (soil / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TemperatureSensorCard(
    temperature: Double?,
    modifier: Modifier = Modifier
) {
    val (tone, label, desc) = when {
        temperature == null -> Triple("blue", "TIDAK TERSEDIA", "Menunggu data suhu")
        temperature < 20.0 -> Triple("blue", "DINGIN", "Suhu lingkungan rendah")
        temperature <= 30.0 -> Triple("green", "NORMAL", "Suhu lingkungan normal")
        else -> Triple("red", "PANAS", "Suhu lingkungan tinggi")
    }
    val accent = toneColor(tone)

    SimpleClimateCard(
        title = "TEMPERATURE",
        valueText = if (temperature != null) "${fmt(temperature)} °C" else "-- °C",
        badgeLabel = label,
        description = desc,
        icon = Icons.Filled.Thermostat,
        accent = accent,
        modifier = modifier
    )
}

@Composable
private fun HumiditySensorCard(
    humidity: Double?,
    modifier: Modifier = Modifier
) {
    val (tone, label, desc) = when {
        humidity == null -> Triple("blue", "TIDAK TERSEDIA", "Menunggu data udara")
        humidity < 40.0 -> Triple("blue", "RENDAH", "Udara relatif kering")
        humidity <= 70.0 -> Triple("green", "NORMAL", "Kelembaban udara normal")
        else -> Triple("red", "TINGGI", "Kelembaban udara tinggi")
    }
    val accent = toneColor(tone)

    SimpleClimateCard(
        title = "HUMIDITY",
        valueText = if (humidity != null) "${fmt(humidity)} %" else "-- %",
        badgeLabel = label,
        description = desc,
        icon = if (humidity != null && humidity > 70.0) Icons.Filled.CloudQueue else Icons.Filled.Cloud,
        accent = accent,
        modifier = modifier
    )
}

@Composable
private fun SimpleClimateCard(
    title: String,
    valueText: String,
    badgeLabel: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatusBadgePill(label = badgeLabel, color = accent)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusBadgePill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SummaryStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private fun fmt(v: Double): String = String.format(Locale.US, "%.1f", v)
