package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.IrrigationZoneEntity
import com.example.ui.state.DailyWaterChartBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureDryAmber
import com.example.ui.theme.MoistureOptimalGreen
import com.example.ui.theme.WaterActiveCyan
import com.example.ui.util.IrrigationFormatters
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SimulationModeBanner(
    isSimulationMode: Boolean,
    controllerUrl: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpenSettings,
        modifier = modifier
            .fillMaxWidth()
            .testTag("mode_status_banner"),
        shape = RoundedCornerShape(14.dp),
        color = if (isSimulationMode) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSimulationMode) Icons.Filled.Science else Icons.Filled.Sensors,
                contentDescription = if (isSimulationMode) "Mode simulasi" else "Kontroler fisik",
                tint = if (isSimulationMode) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSimulationMode) {
                        "Mode Simulasi Perangkat Aktif"
                    } else {
                        "Terhubung ke Kontroler HTTP"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSimulationMode) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
                Text(
                    text = if (isSimulationMode) {
                        "Pengujian lokal tanpa katup fisik. Ketuk untuk hubungkan ESP32/REST."
                    } else {
                        "Target: $controllerUrl"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSimulationMode) {
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ActiveWateringBanner(
    activeZones: List<IrrigationZoneEntity>,
    onStopZone: (IrrigationZoneEntity) -> Unit,
    onStopAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeZones.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "water_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_watering_banner"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(WaterActiveCyan.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${activeZones.size} Zona Sedang Menyiram",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                TextButton(
                    onClick = onStopAll,
                    modifier = Modifier.testTag("banner_stop_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Hentikan semua",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop Semua", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            activeZones.forEach { zone ->
                val elapsedSec = (zone.activeTotalSeconds - zone.remainingSeconds).coerceAtLeast(0)
                val litersSoFar = (elapsedSec / 60f) * zone.flowRateLitersPerMin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = WaterActiveCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = zone.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", litersSoFar)} L mengalir • ${zone.activeTriggerSource}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = IrrigationFormatters.formatCountdown(zone.remainingSeconds),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onStopZone(zone) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("stop_zone_${zone.id}")
                    ) {
                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun MoistureGaugeBar(
    moisturePercent: Int,
    targetMin: Int,
    targetMax: Int,
    modifier: Modifier = Modifier
) {
    val barColor by animateColorAsState(
        targetValue = when {
            moisturePercent < targetMin -> MoistureDryAmber
            moisturePercent <= targetMax -> MoistureOptimalGreen
            else -> WaterActiveCyan
        },
        label = "moisture_color"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    moisturePercent < targetMin -> "Kering (Butuh Air)"
                    moisturePercent <= targetMax -> "Kelembapan Ideal"
                    else -> "Lembap Penuh"
                },
                style = MaterialTheme.typography.labelMedium,
                color = barColor
            )
            Text(
                text = "$moisturePercent% (Target $targetMin–$targetMax%)",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val targetMarkerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(size.height / 2, size.height / 2)
            )
            val fillWidth = (moisturePercent.coerceIn(0, 100) / 100f) * size.width
            drawRoundRect(
                color = barColor,
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2, size.height / 2)
            )
            val minX = (targetMin.coerceIn(0, 100) / 100f) * size.width
            val maxX = (targetMax.coerceIn(0, 100) / 100f) * size.width
            drawLine(
                color = targetMarkerColor,
                start = Offset(minX, 0f),
                end = Offset(minX, size.height),
                strokeWidth = 3f
            )
            drawLine(
                color = targetMarkerColor,
                start = Offset(maxX, 0f),
                end = Offset(maxX, size.height),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun WeeklyWaterChartCard(
    bars: List<DailyWaterChartBar>,
    todayLiters: Float,
    weekLiters: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Konsumsi Air 7 Hari",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Total minggu ini: ${String.format(Locale.US, "%.1f", weekLiters)} Liter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Hari Ini ${String.format(Locale.US, "%.0f", todayLiters)} L",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxLiters = (bars.maxOfOrNull { it.liters } ?: 100f).coerceAtLeast(50f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (bars.isEmpty()) return@Canvas
                val slotWidth = size.width / bars.size
                val barWidth = (slotWidth * 0.46f).coerceAtMost(36.dp.toPx())
                bars.forEachIndexed { index, bar ->
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    // Background slot
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                    val ratio = (bar.liters / maxLiters).coerceIn(0f, 1f)
                    val barHeight = (size.height * ratio).coerceAtLeast(if (bar.liters > 0f) 10f else 0f)
                    if (barHeight > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(secondaryColor, primaryColor)
                            ),
                            topLeft = Offset(left, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bars.forEach { bar ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (bar.liters >= 1f) "${bar.liters.roundToInt()}L" else "0",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickWaterDurationDialog(
    zone: IrrigationZoneEntity,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit
) {
    var selectedMinutes by remember(zone.id) { mutableIntStateOf(zone.defaultDurationMinutes) }
    val presets = listOf(2, 5, 10, 15, 25, 40)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Siram ${zone.name}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "Pilih durasi penyiraman manual (CH ${zone.channelNumber}, debit ${zone.flowRateLitersPerMin} L/mnt).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { mins ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { selectedMinutes = mins },
                            label = { Text("$mins mnt") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Durasi: $selectedMinutes menit (~${String.format(Locale.US, "%.0f", selectedMinutes * zone.flowRateLitersPerMin)} Liter)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.roundToInt().coerceIn(1, 60) },
                    valueRange = 1f..60f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(selectedMinutes) },
                modifier = Modifier.testTag("confirm_start_watering_button")
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mulai Siram")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
