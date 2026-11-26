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
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.remote.GanApiService
import com.example.data.remote.IndonesianLocationPresets
import com.example.data.remote.LocationPreset
import com.example.data.repository.WeatherSnapshot
import com.example.ui.state.GanApiTestState
import com.example.ui.state.GanUiState
import com.example.ui.state.WeatherUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MoistureOptimalGreen
import com.example.ui.util.IrrigationFormatters
import java.util.Locale

@Composable
fun GanSettingsScreen(
    uiState: GanUiState,
    onSaveApiConfig: (String, String) -> Unit,
    onResetDefaultConfig: () -> Unit,
    onTestApiConnection: (String, String) -> Unit,
    onRefreshWeather: () -> Unit,
    onSelectLocationPreset: (LocationPreset) -> Unit,
    onUseGpsLocation: () -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var apiUrl by remember(uiState.config.ganApiUrl) { mutableStateOf(uiState.config.ganApiUrl) }
    var deviceId by remember(uiState.config.ganDeviceId) { mutableStateOf(uiState.config.ganDeviceId) }
    var gpsDenied by remember { mutableStateOf(false) }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            gpsDenied = false
            onUseGpsLocation()
        } else {
            gpsDenied = true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // API & DEVICE CONFIG (from config.php)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Api, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Koneksi API (config.php)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Endpoint PHP & Device ID untuk monitoring & kontrol",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("GAN_API_URL (api.php)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gan_api_url_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = { Text("GAN_DEVICE_ID") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gan_device_id_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestApiConnection(apiUrl, deviceId) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gan_test_api_btn")
                        ) {
                            if (uiState.apiTestState is GanApiTestState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uji API")
                        }
                        Button(
                            onClick = { onSaveApiConfig(apiUrl, deviceId) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gan_save_api_btn")
                        ) {
                            Text("Simpan")
                        }
                    }

                    TextButton(
                        onClick = {
                            apiUrl = GanApiService.DEFAULT_API_URL
                            deviceId = GanApiService.DEFAULT_DEVICE_ID
                            onResetDefaultConfig()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default config.php")
                    }

                    when (val test = uiState.apiTestState) {
                        is GanApiTestState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MoistureOptimalGreen.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MoistureOptimalGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${test.result.message} (${test.result.latencyMs} ms)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MoistureOptimalGreen
                                    )
                                }
                            }
                        }
                        is GanApiTestState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = test.message,
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

        // WEATHER & LOCATION CARD (Open-Meteo)
        item {
            val snap: WeatherSnapshot? = when (val ws = uiState.weatherState) {
                is WeatherUiState.Success -> ws.snapshot
                is WeatherUiState.Error -> ws.cached
                WeatherUiState.Loading -> null
            }
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Prakiraan Cuaca Lokasi Kebun", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = uiState.config.locationName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onRefreshWeather) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Segarkan cuaca")
                        }
                    }

                    if (snap != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Suhu ${String.format(Locale.US, "%.1f", snap.temperatureC)}°C • Lembap ${snap.humidityPercent}% • Peluang Hujan 12j ${snap.nextHoursRainProbability}% • ${IrrigationFormatters.weatherCodeToLabel(snap.weatherCode)}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                onUseGpsLocation()
                            } else {
                                gpsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deteksi Lokasi GPS")
                    }

                    if (gpsDenied) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Izin lokasi ditolak. Pilih kota di bawah ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(IndonesianLocationPresets.cities) { preset ->
                            FilterChip(
                                selected = uiState.config.locationName == preset.name,
                                onClick = { onSelectLocationPreset(preset) },
                                label = { Text(preset.name) }
                            )
                        }
                    }
                }
            }
        }

        // APPEARANCE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Warna Dinamis Material You", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = uiState.config.useDynamicColor,
                        onCheckedChange = onToggleDynamicColor
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
