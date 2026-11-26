package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.IrrigationZoneEntity
import com.example.ui.components.MoistureGaugeBar
import com.example.ui.components.SimulationModeBanner
import com.example.ui.state.IrrigationUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.util.IrrigationFormatters
import kotlin.math.roundToInt

@Composable
fun ZonesScreen(
    uiState: IrrigationUiState,
    onQuickWaterZone: (IrrigationZoneEntity) -> Unit,
    onStopZone: (IrrigationZoneEntity) -> Unit,
    onSaveZone: (IrrigationZoneEntity) -> Unit,
    onDeleteZone: (IrrigationZoneEntity) -> Unit,
    onAdjustSimulationMoisture: (IrrigationZoneEntity, Int) -> Unit,
    onNavigateToSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingZone by remember { mutableStateOf<IrrigationZoneEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.zones.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_smart_valve),
                    contentDescription = "Ilustrasi katup pintar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Belum Ada Zona Irigasi",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tambahkan zona katup (CH 1–8) untuk mulai memantau kelembapan tanah dan penyiraman otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("empty_add_zone_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Zona Pertama")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SimulationModeBanner(
                        isSimulationMode = uiState.config.isSimulationMode,
                        controllerUrl = uiState.config.controllerUrl,
                        onOpenSettings = onNavigateToSystem
                    )
                }

                items(uiState.zones, key = { it.id }) { zone ->
                    ZoneDetailCard(
                        zone = zone,
                        isSimulationMode = uiState.config.isSimulationMode,
                        onStart = { onQuickWaterZone(zone) },
                        onStop = { onStopZone(zone) },
                        onEdit = { editingZone = zone },
                        onDelete = { onDeleteZone(zone) },
                        onAdjustMoisture = { delta -> onAdjustSimulationMoisture(zone, delta) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_zone_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah zona")
        }
    }

    if (showAddDialog) {
        val nextChannel = (uiState.zones.maxOfOrNull { it.channelNumber } ?: 0) + 1
        ZoneEditorDialog(
            initial = IrrigationZoneEntity(
                name = "",
                plantType = "",
                channelNumber = nextChannel.coerceAtMost(8),
                soilMoisturePercent = 48,
                targetMoistureMin = 45,
                targetMoistureMax = 75,
                flowRateLitersPerMin = 10f,
                defaultDurationMinutes = 10
            ),
            isNew = true,
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveZone(it)
                showAddDialog = false
            }
        )
    }

    editingZone?.let { target ->
        ZoneEditorDialog(
            initial = target,
            isNew = false,
            onDismiss = { editingZone = null },
            onSave = {
                onSaveZone(it)
                editingZone = null
            }
        )
    }
}

@Composable
private fun ZoneDetailCard(
    zone: IrrigationZoneEntity,
    isSimulationMode: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustMoisture: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zone_card_${zone.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (zone.isValveOpen) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (zone.isValveOpen) Icons.Filled.WaterDrop else Icons.Filled.Grass,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (zone.isValveOpen) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CH ${zone.channelNumber}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${zone.plantType} • ${zone.flowRateLitersPerMin} L/mnt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_zone_${zone.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Ubah zona")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_zone_${zone.id}")
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Hapus zona",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MoistureGaugeBar(
                moisturePercent = zone.soilMoisturePercent,
                targetMin = zone.targetMoistureMin,
                targetMax = zone.targetMoistureMax
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Terakhir: ${IrrigationFormatters.formatRelativeTime(zone.lastWateredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (zone.autoMoistureEnabled) "Otomatis Kelembapan: Aktif" else "Otomatis Kelembapan: Nonaktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (zone.isValveOpen) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("zone_stop_btn_${zone.id}")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(IrrigationFormatters.formatCountdown(zone.remainingSeconds))
                    }
                } else {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("zone_start_btn_${zone.id}")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Siram ${zone.defaultDurationMinutes}m")
                    }
                }
            }

            if (isSimulationMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uji Sensor:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { onAdjustMoisture(-10) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("sim_dry_${zone.id}")
                    ) {
                        Text("-10% Kering", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onAdjustMoisture(10) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("sim_wet_${zone.id}")
                    ) {
                        Text("+10% Basah", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneEditorDialog(
    initial: IrrigationZoneEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (IrrigationZoneEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var plantType by remember { mutableStateOf(initial.plantType) }
    var channel by remember { mutableIntStateOf(initial.channelNumber) }
    var targetMin by remember { mutableIntStateOf(initial.targetMoistureMin) }
    var targetMax by remember { mutableIntStateOf(initial.targetMoistureMax) }
    var defaultDuration by remember { mutableIntStateOf(initial.defaultDurationMinutes) }
    var flowRateText by remember { mutableStateOf(initial.flowRateLitersPerMin.toString()) }
    var autoEnabled by remember { mutableStateOf(initial.autoMoistureEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Tambah Zona Irigasi" else "Ubah Zona")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Zona (mis. Taman Depan)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zone_name_input")
                )
                OutlinedTextField(
                    value = plantType,
                    onValueChange = { plantType = it },
                    label = { Text("Jenis Tanaman") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zone_plant_input")
                )
                OutlinedTextField(
                    value = flowRateText,
                    onValueChange = { flowRateText = it },
                    label = { Text("Debit Air (Liter/menit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Channel Katup: CH $channel",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = channel.toFloat(),
                    onValueChange = { channel = it.roundToInt().coerceIn(1, 8) },
                    valueRange = 1f..8f,
                    steps = 6
                )
                Text(
                    text = "Durasi Default: $defaultDuration menit",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = defaultDuration.toFloat(),
                    onValueChange = { defaultDuration = it.roundToInt().coerceIn(1, 60) },
                    valueRange = 1f..60f
                )
                Text(
                    text = "Target Kelembapan: $targetMin% – $targetMax%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = targetMin.toFloat(),
                    onValueChange = {
                        val nextMin = it.roundToInt().coerceIn(15, 75)
                        targetMin = nextMin
                        if (targetMax < nextMin + 10) targetMax = (nextMin + 15).coerceAtMost(95)
                    },
                    valueRange = 15f..75f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Otomatis jika tanah kering",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = autoEnabled,
                        onCheckedChange = { autoEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim().ifBlank { "Zona CH $channel" }
                    val cleanPlant = plantType.trim().ifBlank { "Tanaman Umum" }
                    val flow = flowRateText.toFloatOrNull()?.coerceIn(1f, 60f) ?: 10f
                    onSave(
                        initial.copy(
                            name = cleanName,
                            plantType = cleanPlant,
                            channelNumber = channel,
                            targetMoistureMin = targetMin,
                            targetMoistureMax = targetMax,
                            flowRateLitersPerMin = flow,
                            defaultDurationMinutes = defaultDuration,
                            autoMoistureEnabled = autoEnabled
                        )
                    )
                },
                modifier = Modifier.testTag("save_zone_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
