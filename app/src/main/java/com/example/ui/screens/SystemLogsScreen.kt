package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.WateringLogEntity
import com.example.data.remote.IndonesianLocationPresets
import com.example.data.remote.LocationPreset
import com.example.ui.state.ConnectionTestState
import com.example.ui.state.IrrigationUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureOptimalGreen
import com.example.ui.theme.RainSkipBlue
import com.example.ui.util.IrrigationFormatters
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SystemLogsScreen(
    uiState: IrrigationUiState,
    onSaveControllerSettings: (String, String, Boolean) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onSelectLocationPreset: (LocationPreset) -> Unit,
    onUseGpsLocation: () -> Unit,
    onUpdateRainSkip: (Boolean, Int) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var controllerUrl by remember(uiState.config.controllerUrl) {
        mutableStateOf(uiState.config.controllerUrl)
    }
    var apiToken by remember(uiState.config.apiToken) {
        mutableStateOf(uiState.config.apiToken)
    }
    var isSimulation by remember(uiState.config.isSimulationMode) {
        mutableStateOf(uiState.config.isSimulationMode)
    }
    var rainThreshold by remember(uiState.config.rainThresholdPercent) {
        mutableIntStateOf(uiState.config.rainThresholdPercent)
    }
    var selectedLogFilter by remember { mutableStateOf("Semua") }
    var locationPermissionDeniedMsg by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locationPermissionDeniedMsg = false
            onUseGpsLocation()
        } else {
            locationPermissionDeniedMsg = true
        }
    }

    val filterOptions = listOf("Semua", "Manual", "Jadwal", "Otomatis", "Tunda Hujan")
    val filteredLogs = remember(uiState.logs, selectedLogFilter) {
        if (selectedLogFilter == "Semua") uiState.logs
        else uiState.logs.filter { it.triggerSource.equals(selectedLogFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSimulation) Icons.Filled.Science else Icons.Filled.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pengontrol Katup IoT (ESP32 / REST)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Simulasi Perangkat",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isSimulation) {
                                    "Aktif: Uji katup & sensor secara lokal tanpa perangkat keras."
                                } else {
                                    "Nonaktif: Mengirim perintah HTTP langsung ke kontroler Wi-Fi."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSimulation,
                            onCheckedChange = {
                                isSimulation = it
                                onSaveControllerSettings(controllerUrl, apiToken, it)
                            },
                            modifier = Modifier.testTag("simulation_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = controllerUrl,
                        onValueChange = { controllerUrl = it },
                        label = { Text("URL Kontroler (mis. http://192.168.1.50)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("controller_url_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("Token Bearer / API Key (opsional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("controller_token_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestConnection(controllerUrl, apiToken) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_connection_button")
                        ) {
                            if (uiState.connectionTestState is ConnectionTestState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uji Koneksi")
                        }
                        Button(
                            onClick = { onSaveControllerSettings(controllerUrl, apiToken, isSimulation) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_controller_button")
                        ) {
                            Text("Simpan")
                        }
                    }

                    when (val testState = uiState.connectionTestState) {
                        is ConnectionTestState.Success -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MoistureOptimalGreen.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MoistureOptimalGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${testState.result.message} (${testState.result.latencyMs} ms)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MoistureOptimalGreen
                                    )
                                }
                            }
                        }
                        is ConnectionTestState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = testState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lokasi Kebun & Otomasi Hujan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Aktif: ${uiState.config.locationName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                onUseGpsLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_gps_button")
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gunakan Lokasi GPS Perangkat")
                    }

                    if (locationPermissionDeniedMsg) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Izin lokasi ditolak. Pilih kota Indonesia di bawah ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(IndonesianLocationPresets.cities) { preset ->
                            FilterChip(
                                selected = uiState.config.locationName == preset.name,
                                onClick = { onSelectLocationPreset(preset) },
                                label = { Text(preset.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Umbrella, contentDescription = null, tint = RainSkipBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tunda Hujan Otomatis", style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = uiState.config.rainSkipEnabled,
                            onCheckedChange = { onUpdateRainSkip(it, rainThreshold) },
                            modifier = Modifier.testTag("rain_skip_switch")
                        )
                    }
                    Text(
                        text = "Ambang peluang hujan: $rainThreshold%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = rainThreshold.toFloat(),
                        onValueChange = { rainThreshold = it.roundToInt().coerceIn(20, 95) },
                        onValueChangeFinished = { onUpdateRainSkip(uiState.config.rainSkipEnabled, rainThreshold) },
                        valueRange = 20f..95f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Warna Dinamis Android (Material You)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = uiState.config.useDynamicColor,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Riwayat Penyiraman", style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.logs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bersihkan")
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { filter ->
                    FilterChip(
                        selected = selectedLogFilter == filter,
                        onClick = { selectedLogFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Belum ada catatan untuk filter \"$selectedLogFilter\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                WateringLogRowCard(log = log)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun WateringLogRowCard(log: WateringLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.zoneName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${IrrigationFormatters.formatTimestamp(log.startedAt)} • ${log.triggerSource}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (log.litersUsed > 0f) {
                        "${String.format(Locale.US, "%.1f", log.litersUsed)} L"
                    } else {
                        log.status
                    },
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        log.status.contains("Gagal", ignoreCase = true) -> MaterialTheme.colorScheme.error
                        log.status.contains("Hujan", ignoreCase = true) -> RainSkipBlue
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = if (log.durationSeconds > 0) {
                        "${IrrigationFormatters.formatDurationReadable(log.durationSeconds)} • ${log.status}"
                    } else {
                        log.status
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
