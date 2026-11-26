package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.GanInsertPayload
import com.example.data.remote.GanSensorReadingDto
import com.example.ui.state.GanUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureOptimalGreen
import java.util.Locale

@Composable
fun GanHistoryScreen(
    uiState: GanUiState,
    onRefreshHistory: () -> Unit,
    onSubmitSensorData: (GanInsertPayload) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showInsertDialog by remember { mutableStateOf(false) }

    val filteredRows = remember(uiState.historyRows, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) uiState.historyRows
        else uiState.historyRows.filter { row ->
            (row.createdAt?.contains(q, ignoreCase = true) == true) ||
                (row.sensorStatus?.contains(q, ignoreCase = true) == true) ||
                row.waterLevel.toString().contains(q) ||
                row.soilMoisture.toString().contains(q)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari waktu / status sensor...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_search_input")
                    )
                    IconButton(
                        onClick = onRefreshHistory,
                        modifier = Modifier.testTag("refresh_history_btn")
                    ) {
                        if (uiState.isLoadingHistory) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh histori")
                        }
                    }
                }
            }

            if (filteredRows.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = if (uiState.isLoadingHistory) {
                                "Memuat data histori dari api.php..."
                            } else {
                                "Belum ada data histori sensor dari ${uiState.config.ganDeviceId}."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(filteredRows, key = { idx, item -> item.id ?: idx.toLong() }) { idx, row ->
                    HistoryReadingCard(index = idx + 1, row = row)
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showInsertDialog = true },
            icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
            text = { Text("Kirim Data") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("send_sensor_data_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    if (showInsertDialog) {
        InsertSensorDialog(
            deviceId = uiState.config.ganDeviceId,
            latest = uiState.latestReading,
            isSubmitting = uiState.isSubmittingSensor,
            onDismiss = { showInsertDialog = false },
            onSubmit = { payload ->
                onSubmitSensorData(payload)
                showInsertDialog = false
            }
        )
    }
}

@Composable
private fun HistoryReadingCard(index: Int, row: GanSensorReadingDto) {
    val waterPumpOn = row.waterPump == 1
    val soilPumpOn = row.soilPump == 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$index • ${row.createdAt ?: "-"}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PumpMiniTag(label = "WP", isOn = waterPumpOn)
                    PumpMiniTag(label = "SP", isOn = soilPumpOn)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniCell("Water", "${fmt1(row.waterLevel)}%", Modifier.weight(1f))
                MetricMiniCell("Soil", "${fmt1(row.soilMoisture)}%", Modifier.weight(1f))
                MetricMiniCell("Suhu", "${fmt1(row.temperature)}°C", Modifier.weight(1f))
                MetricMiniCell("Lembap", "${fmt1(row.humidity)}%", Modifier.weight(1f))
            }

            if (!row.sensorStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = row.sensorStatus,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PumpMiniTag(label: String, isOn: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isOn) MoistureOptimalGreen else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "$label:${if (isOn) "ON" else "OFF"}",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MetricMiniCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun InsertSensorDialog(
    deviceId: String,
    latest: GanSensorReadingDto?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (GanInsertPayload) -> Unit
) {
    var waterText by remember { mutableStateOf((latest?.waterLevel ?: 75.0).toString()) }
    var soilText by remember { mutableStateOf((latest?.soilMoisture ?: 62.0).toString()) }
    var tempText by remember { mutableStateOf((latest?.temperature ?: 28.5).toString()) }
    var humText by remember { mutableStateOf((latest?.humidity ?: 65.0).toString()) }
    var waterPump by remember { mutableStateOf((latest?.waterPump ?: 0) == 1) }
    var soilPump by remember { mutableStateOf((latest?.soilPump ?: 0) == 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kirim Data ke api.php (insert)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Device ID: $deviceId",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = waterText,
                        onValueChange = { waterText = it },
                        label = { Text("Water Level (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = soilText,
                        onValueChange = { soilText = it },
                        label = { Text("Soil (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        label = { Text("Temp (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = humText,
                        onValueChange = { humText = it },
                        label = { Text("Humidity (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Water Pump ON", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = waterPump, onCheckedChange = { waterPump = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Soil Pump ON", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = soilPump, onCheckedChange = { soilPump = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    val w = (waterText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                    val s = (soilText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                    val t = (tempText.toDoubleOrNull() ?: 27.0).coerceIn(-40.0, 100.0)
                    val h = (humText.toDoubleOrNull() ?: 60.0).coerceIn(0.0, 100.0)
                    onSubmit(
                        GanInsertPayload(
                            deviceId = deviceId,
                            waterLevel = w,
                            soilMoisture = s,
                            temperature = t,
                            humidity = h,
                            rawWater = (w * 40).toInt(),
                            rawSoil = (s * 40).toInt(),
                            waterPump = if (waterPump) 1 else 0,
                            soilPump = if (soilPump) 1 else 0
                        )
                    )
                },
                modifier = Modifier.testTag("confirm_insert_sensor_btn")
            ) {
                Text("Kirim ke API")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
