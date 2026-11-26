package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.data.remote.GanControlConfigDto
import com.example.ui.state.GanUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureOptimalGreen

@Composable
fun GanControlScreen(
    uiState: GanUiState,
    onUpdateDraft: ((GanControlConfigDto) -> GanControlConfigDto) -> Unit,
    onSaveControl: () -> Unit,
    onRefreshControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctrl = uiState.controlConfig
    val liveWaterPumpOn = (uiState.latestReading?.waterPump ?: ctrl.waterPump) == 1
    val liveSoilPumpOn = (uiState.latestReading?.soilPump ?: ctrl.soilPump) == 1

    var waterOnText by remember(ctrl.waterOnAt) { mutableStateOf(ctrl.waterOnAt.toInt().toString()) }
    var waterOffText by remember(ctrl.waterOffAt) { mutableStateOf(ctrl.waterOffAt.toInt().toString()) }
    var soilOnText by remember(ctrl.soilOnAt) { mutableStateOf(ctrl.soilOnAt.toInt().toString()) }
    var soilOffText by remember(ctrl.soilOffAt) { mutableStateOf(ctrl.soilOffAt.toInt().toString()) }
    var timerStartText by remember(ctrl.timerStart) { mutableStateOf(ctrl.timerStart) }
    var timerEndText by remember(ctrl.timerEnd) { mutableStateOf(ctrl.timerEnd) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // WATER PUMP PANEL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PumpHeaderRow(
                        title = "WATER PUMP",
                        subtitle = "Pompa pengisian tandon air",
                        isOn = liveWaterPumpOn,
                        icon = { Icon(Icons.Filled.Water, contentDescription = null, tint = Color(0xFF0284C7)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Mode Operasional", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO", "MANUAL", "TIMER").forEach { mode ->
                            FilterChip(
                                selected = ctrl.waterMode == mode,
                                onClick = { onUpdateDraft { it.copy(waterMode = mode) } },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("water_mode_$mode")
                            )
                        }
                    }

                    if (ctrl.waterMode == "MANUAL") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pompa Air ON/OFF", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = ctrl.waterPump == 1,
                                onCheckedChange = { checked ->
                                    onUpdateDraft { it.copy(waterPump = if (checked) 1 else 0) }
                                },
                                modifier = Modifier.testTag("water_pump_manual_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = waterOnText,
                            onValueChange = { raw ->
                                waterOnText = raw
                                raw.toDoubleOrNull()?.let { v -> onUpdateDraft { it.copy(waterOnAt = v) } }
                            },
                            label = { Text("ON jika level <= (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = waterOffText,
                            onValueChange = { raw ->
                                waterOffText = raw
                                raw.toDoubleOrNull()?.let { v -> onUpdateDraft { it.copy(waterOffAt = v) } }
                            },
                            label = { Text("OFF jika level >= (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // SOIL PUMP PANEL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PumpHeaderRow(
                        title = "SOIL IRRIGATION PUMP",
                        subtitle = "Pompa penyiraman tanaman",
                        isOn = liveSoilPumpOn,
                        icon = { Icon(Icons.Filled.Eco, contentDescription = null, tint = MoistureOptimalGreen) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Mode Operasional", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO", "MANUAL").forEach { mode ->
                            FilterChip(
                                selected = ctrl.soilMode == mode,
                                onClick = { onUpdateDraft { it.copy(soilMode = mode) } },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("soil_mode_$mode")
                            )
                        }
                    }

                    if (ctrl.soilMode == "MANUAL") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pompa Penyiraman ON/OFF", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = ctrl.soilPump == 1,
                                onCheckedChange = { checked ->
                                    onUpdateDraft { it.copy(soilPump = if (checked) 1 else 0) }
                                },
                                modifier = Modifier.testTag("soil_pump_manual_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = soilOnText,
                            onValueChange = { raw ->
                                soilOnText = raw
                                raw.toDoubleOrNull()?.let { v -> onUpdateDraft { it.copy(soilOnAt = v) } }
                            },
                            label = { Text("ON jika tanah <= (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = soilOffText,
                            onValueChange = { raw ->
                                soilOffText = raw
                                raw.toDoubleOrNull()?.let { v -> onUpdateDraft { it.copy(soilOffAt = v) } }
                            },
                            label = { Text("OFF jika tanah >= (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // TIMER PANEL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TIMER PENYIRAMAN POMPA AIR",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Switch(
                            checked = ctrl.timerEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateDraft { it.copy(timerEnabled = enabled) }
                            },
                            modifier = Modifier.testTag("timer_enabled_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = timerStartText,
                            onValueChange = { raw ->
                                timerStartText = raw
                                onUpdateDraft { it.copy(timerStart = raw) }
                            },
                            label = { Text("Jam Mulai (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = timerEndText,
                            onValueChange = { raw ->
                                timerEndText = raw
                                onUpdateDraft { it.copy(timerEnd = raw) }
                            },
                            label = { Text("Jam Selesai (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Timer berjalan sesuai waktu WIB pada ESP32.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // SAVE & REFRESH BUTTONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveControl,
                    enabled = !uiState.isSavingControl,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("save_control_to_api_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isSavingControl) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMPAN PENGATURAN")
                }

                OutlinedButton(
                    onClick = onRefreshControl,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("refresh_control_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REFRESH")
                }
            }
        }

        if (!uiState.controlStatusMessage.isNullOrBlank()) {
            item {
                val isErr = uiState.controlStatusMessage.startsWith("Gagal", true) ||
                    uiState.controlStatusMessage.startsWith("Periksa", true) ||
                    uiState.controlStatusMessage.startsWith("Format", true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isErr) MaterialTheme.colorScheme.errorContainer else MoistureOptimalGreen.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.controlStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isErr) MaterialTheme.colorScheme.onErrorContainer else MoistureOptimalGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun PumpHeaderRow(
    title: String,
    subtitle: String,
    isOn: Boolean,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            icon()
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isOn) MoistureOptimalGreen else AlertRed.copy(alpha = 0.16f)
        ) {
            Text(
                text = if (isOn) "ON" else "OFF",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = if (isOn) Color.White else AlertRed,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}
