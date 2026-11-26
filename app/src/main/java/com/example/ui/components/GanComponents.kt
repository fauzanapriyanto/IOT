package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureDryAmber
import com.example.ui.theme.MoistureOptimalGreen
import com.example.ui.theme.RainSkipBlue

@Composable
fun GanServerStatusBanner(
    deviceId: String,
    apiUrl: String,
    isOnline: Boolean,
    lastUpdate: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gan_server_status_banner"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) MoistureOptimalGreen else AlertRed)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOnline) "SERVER ONLINE" else "SERVER OFFLINE",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOnline) MoistureOptimalGreen else AlertRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $deviceId",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (!lastUpdate.isNullOrBlank()) {
                        "Update terakhir: $lastUpdate"
                    } else {
                        "Endpoint: $apiUrl"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("gan_refresh_all_btn")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh data server")
            }
        }
    }
}

@Composable
fun AnimatedWaterTankGauge(
    waterPercent: Double,
    modifier: Modifier = Modifier
) {
    val clamped = waterPercent.coerceIn(0.0, 100.0).toFloat()
    val animatedLevel by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 550),
        label = "tank_fill"
    )

    val (topColor, bottomColor) = when {
        clamped <= 25f -> Color(0xFFF87171) to Color(0xFFDC2626)
        clamped <= 50f -> Color(0xFFFDE047) to Color(0xFFEAB308)
        clamped < 95f -> Color(0xFF4ADE80) to Color(0xFF16A34A)
        else -> Color(0xFF38BDF8) to Color(0xFF2563EB)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(68.dp)
                .height(104.dp)
        ) {
            val neckWidth = size.width * 0.72f
            val neckLeft = (size.width - neckWidth) / 2f
            val neckHeight = 8.dp.toPx()

            // Tank cap
            drawRoundRect(
                color = Color(0xFFCBD5E1),
                topLeft = Offset(neckLeft, 0f),
                size = Size(neckWidth, neckHeight + 3.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF64748B),
                topLeft = Offset(neckLeft, 0f),
                size = Size(neckWidth, neckHeight + 3.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            val bodyTop = neckHeight
            val bodyHeight = size.height - bodyTop
            // Tank shell background
            drawRoundRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(2.dp.toPx(), bodyTop),
                size = Size(size.width - 4.dp.toPx(), bodyHeight - 2.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )

            // Liquid fill inside padding
            val innerLeft = 5.dp.toPx()
            val innerWidth = size.width - 10.dp.toPx()
            val innerMaxHeight = bodyHeight - 8.dp.toPx()
            val fillHeight = (animatedLevel / 100f) * innerMaxHeight
            if (fillHeight > 2f) {
                val fillTop = size.height - 5.dp.toPx() - fillHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
                    topLeft = Offset(innerLeft, fillTop),
                    size = Size(innerWidth, fillHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                // Surface sheen
                drawOval(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(innerLeft + 3.dp.toPx(), fillTop + 1.dp.toPx()),
                    size = Size((innerWidth - 6.dp.toPx()).coerceAtLeast(4f), 4.dp.toPx())
                )
            }

            // Tank border outline
            drawRoundRect(
                color = Color(0xFF64748B),
                topLeft = Offset(2.dp.toPx(), bodyTop),
                size = Size(size.width - 4.dp.toPx(), bodyHeight - 2.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.height(96.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
                Text(
                    text = tick,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class ChartSeriesSpec(
    val label: String,
    val color: Color,
    val values: List<Float>,
    val minRange: Float = 0f,
    val maxRange: Float = 100f
)

@Composable
fun DualLineSensorChartCard(
    title: String,
    seriesA: ChartSeriesSpec,
    seriesB: ChartSeriesSpec,
    timeLabels: List<String>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(label = seriesA.label, color = seriesA.color)
                LegendChip(label = seriesB.label, color = seriesB.color)
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (seriesA.values.isEmpty() && seriesB.values.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Menunggu data histori dari api.php...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                ) {
                    // Horizontal grid lines (0%, 25%, 50%, 75%, 100%)
                    for (i in 0..4) {
                        val y = size.height * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun drawSeries(spec: ChartSeriesSpec) {
                        val vals = spec.values
                        if (vals.isEmpty()) return
                        val span = (spec.maxRange - spec.minRange).coerceAtLeast(1f)
                        val count = vals.size
                        val stepX = if (count > 1) size.width / (count - 1) else size.width

                        val linePath = Path()
                        val fillPath = Path()
                        vals.forEachIndexed { idx, v ->
                            val x = if (count > 1) idx * stepX else size.width / 2f
                            val norm = ((v - spec.minRange) / span).coerceIn(0f, 1f)
                            val y = size.height - (norm * size.height)
                            if (idx == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, size.height)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (idx == vals.lastIndex) {
                                fillPath.lineTo(x, size.height)
                                fillPath.close()
                            }
                        }

                        if (count > 1) {
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(spec.color.copy(alpha = 0.22f), Color.Transparent)
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = spec.color,
                                style = Stroke(
                                    width = 2.6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else {
                            val norm = ((vals.first() - spec.minRange) / span).coerceIn(0f, 1f)
                            drawCircle(
                                color = spec.color,
                                radius = 5.dp.toPx(),
                                center = Offset(size.width / 2f, size.height - norm * size.height)
                            )
                        }
                    }

                    drawSeries(seriesA)
                    drawSeries(seriesB)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = timeLabels.firstOrNull().orEmpty(),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (timeLabels.size > 2) {
                        Text(
                            text = timeLabels[timeLabels.size / 2],
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = timeLabels.lastOrNull().orEmpty(),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun toneColor(tone: String): Color = when (tone) {
    "red" -> AlertRed
    "yellow" -> MoistureDryAmber
    "green" -> MoistureOptimalGreen
    else -> RainSkipBlue
}
