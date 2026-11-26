package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.IrrigationZoneEntity
import com.example.data.repository.WeatherSnapshot
import com.example.ui.components.ActiveWateringBanner
import com.example.ui.components.MoistureGaugeBar
import com.example.ui.components.SimulationModeBanner
import com.example.ui.components.WeeklyWaterChartCard
import com.example.ui.state.IrrigationUiState
import com.example.ui.state.WeatherUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RainSkipBlue
import com.example.ui.theme.WaterActiveCyan
import com.example.ui.util.IrrigationFormatters
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: IrrigationUiState,
    onRefreshWeather: () -> Unit,
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onRunSmartCheck: () -> Unit,
    onQuickWaterZone: (IrrigationZoneEntity) -> Unit,
    onStopZone: (IrrigationZoneEntity) -> Unit,
    onNavigateToZones: () -> Unit,
    onNavigateToSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeZones = uiState.zones.filter { it.isValveOpen }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroIrrigationCard(
                uiState = uiState,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            SimulationModeBanner(
                isSimulationMode = uiState.config.isSimulationMode,
                controllerUrl = uiState.config.controllerUrl,
                onOpenSettings = onNavigateToSystem
            )
        }

        if (activeZones.isNotEmpty()) {
            item {
                ActiveWateringBanner(
                    activeZones = activeZones,
                    onStopZone = onStopZone,
                    onStopAll = onStopAll
                )
            }
        }

        item {
            QuickControlRow(
                hasActiveValves = activeZones.isNotEmpty(),
                onStartAll = onStartAll,
                onStopAll = onStopAll,
                onRunSmartCheck = onRunSmartCheck
            )
        }

        item {
            WeatherForecastCard(
                weatherState = uiState.weatherState,
                rainSkipEnabled = uiState.config.rainSkipEnabled,
                rainThreshold = uiState.config.rainThresholdPercent,
                shouldRainSkipNow = uiState.shouldRainSkipNow,
                onRefresh = onRefreshWeather
            )
        }

        item {
            WeeklyWaterChartCard(
                bars = uiState.weeklyWaterChart,
                todayLiters = uiState.todayTotalLiters,
                weekLiters = uiState.weekTotalLiters
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Kelembapan Zona",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToZones() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("see_all_zones_button")
                )
            }
        }

        items(uiState.zones, key = { it.id }) { zone ->
            DashboardZoneMiniCard(
                zone = zone,
                onQuickWater = { onQuickWaterZone(zone) },
                onStop = { onStopZone(zone) }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun HeroIrrigationCard(
    uiState: IrrigationUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_garden),
            contentDescription = "Latar kebun smart irrigation",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF042622).copy(alpha = 0.35f),
                            Color(0xFF042622).copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = uiState.config.locationName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (uiState.activeZoneCount > 0) WaterActiveCyan else Color(0xFF16A34A)
                ) {
                    Text(
                        text = if (uiState.activeZoneCount > 0) {
                            "MENYIRAM (${uiState.activeZoneCount})"
                        } else {
                            "SISTEM SIAGA"
                        },
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "IrigasiKu Smart Control",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetricPill(
                        label = "Rata Kelembapan",
                        value = "${uiState.averageMoisturePercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = "Zona Aktif",
                        value = "${uiState.zones.size} Zona",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = "Air Hari Ini",
                        value = "${String.format(Locale.US, "%.0f", uiState.todayTotalLiters)} L",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QuickControlRow(
    hasActiveValves: Boolean,
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onRunSmartCheck: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartAll,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_start_all_button"),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Siram 5m", maxLines = 1)
        }

        if (hasActiveValves) {
            Button(
                onClick = onStopAll,
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_stop_all_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop", maxLines = 1)
            }
        } else {
            OutlinedButton(
                onClick = onStopAll,
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_stop_all_button"),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop", maxLines = 1)
            }
        }

        FilledTonalButton(
            onClick = onRunSmartCheck,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_auto_check_button"),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Otomasi", maxLines = 1)
        }
    }
}

@Composable
private fun WeatherForecastCard(
    weatherState: WeatherUiState,
    rainSkipEnabled: Boolean,
    rainThreshold: Int,
    shouldRainSkipNow: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cuaca & Evapotranspirasi (Open-Meteo)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val syncText = when (weatherState) {
                        is WeatherUiState.Success -> IrrigationFormatters.formatSyncTime(weatherState.snapshot.fetchedAtMillis)
                        is WeatherUiState.Error -> weatherState.message
                        WeatherUiState.Loading -> "Mengambil data cuaca terbaru..."
                    }
                    Text(
                        text = syncText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (weatherState is WeatherUiState.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_weather_button")
                ) {
                    if (weatherState is WeatherUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Segarkan cuaca")
                    }
                }
            }

            val snapshot: WeatherSnapshot? = when (weatherState) {
                is WeatherUiState.Success -> weatherState.snapshot
                is WeatherUiState.Error -> weatherState.cached
                WeatherUiState.Loading -> null
            }

            if (snapshot != null) {
                Spacer(modifier = Modifier.height(12.dp))

                if (rainSkipEnabled && shouldRainSkipNow) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RainSkipBlue.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Umbrella,
                                contentDescription = "Tunda hujan aktif",
                                tint = RainSkipBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tunda Hujan Aktif: Peluang hujan ${snapshot.nextHoursRainProbability}% (>= $rainThreshold%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = RainSkipBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherStatChip(
                        icon = Icons.Filled.Thermostat,
                        label = "Suhu",
                        value = "${String.format(Locale.US, "%.1f", snapshot.temperatureC)}°C",
                        modifier = Modifier.weight(1f)
                    )
                    WeatherStatChip(
                        icon = Icons.Filled.Opacity,
                        label = "Lembap",
                        value = "${snapshot.humidityPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    WeatherStatChip(
                        icon = Icons.Filled.Umbrella,
                        label = "Hujan 12j",
                        value = "${snapshot.nextHoursRainProbability}%",
                        modifier = Modifier.weight(1f)
                    )
                    WeatherStatChip(
                        icon = Icons.Filled.Air,
                        label = "ET₀",
                        value = "${String.format(Locale.US, "%.1f", snapshot.todayEt0Mm)}mm",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (snapshot.forecastDays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.forecastDays) { day ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = IrrigationFormatters.formatApiDayShort(day.date),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = if (day.rainProbabilityMax >= 55) Icons.Filled.CloudQueue else Icons.Filled.WbSunny,
                                        contentDescription = IrrigationFormatters.weatherCodeToLabel(day.weatherCode),
                                        tint = if (day.rainProbabilityMax >= 55) RainSkipBlue else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${day.tempMax.toInt()}° / ${day.rainProbabilityMax}%",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
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

@Composable
private fun DashboardZoneMiniCard(
    zone: IrrigationZoneEntity,
    onQuickWater: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "CH ${zone.channelNumber} • ${zone.plantType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (zone.isValveOpen) {
                    FilledTonalButton(
                        onClick = onStop,
                        modifier = Modifier.testTag("dash_stop_zone_${zone.id}")
                    ) {
                        Text(IrrigationFormatters.formatCountdown(zone.remainingSeconds))
                    }
                } else {
                    FilledTonalButton(
                        onClick = onQuickWater,
                        modifier = Modifier.testTag("dash_water_zone_${zone.id}")
                    ) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Siram")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            MoistureGaugeBar(
                moisturePercent = zone.soilMoisturePercent,
                targetMin = zone.targetMoistureMin,
                targetMax = zone.targetMoistureMax
            )
        }
    }
}
