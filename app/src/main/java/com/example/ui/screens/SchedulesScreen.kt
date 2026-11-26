package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.IrrigationScheduleEntity
import com.example.data.local.IrrigationZoneEntity
import com.example.ui.state.IrrigationUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RainSkipBlue
import com.example.ui.util.IrrigationFormatters
import kotlin.math.roundToInt

@Composable
fun SchedulesScreen(
    uiState: IrrigationUiState,
    onSaveSchedule: (IrrigationScheduleEntity) -> Unit,
    onToggleSchedule: (IrrigationScheduleEntity, Boolean) -> Unit,
    onDeleteSchedule: (IrrigationScheduleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<IrrigationScheduleEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.schedules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Belum Ada Jadwal Otomatis",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Atur jadwal penyiraman rutin pagi atau sore dengan dukungan tunda hujan otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.shouldRainSkipNow) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RainSkipBlue.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Umbrella,
                                    contentDescription = null,
                                    tint = RainSkipBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Prakiraan hujan tinggi terdeteksi. Jadwal bertanda \"Tunda Hujan\" akan dilewati otomatis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RainSkipBlue
                                )
                            }
                        }
                    }
                }

                items(uiState.schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { enabled -> onToggleSchedule(schedule, enabled) },
                        onEdit = { editingSchedule = schedule },
                        onDelete = { onDeleteSchedule(schedule) }
                    )
                }
            }
        }

        if (uiState.zones.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_schedule_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah jadwal")
            }
        }
    }

    if (showAddDialog && uiState.zones.isNotEmpty()) {
        val defaultZone = uiState.zones.first()
        ScheduleEditorDialog(
            initial = IrrigationScheduleEntity(
                zoneId = defaultZone.id,
                zoneName = defaultZone.name,
                label = "Jadwal Siram Pagi",
                hour = 6,
                minute = 30,
                durationMinutes = defaultZone.defaultDurationMinutes,
                daysMask = 0b1111111,
                skipOnRain = true,
                isEnabled = true
            ),
            zones = uiState.zones,
            isNew = true,
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveSchedule(it)
                showAddDialog = false
            }
        )
    }

    editingSchedule?.let { target ->
        if (uiState.zones.isNotEmpty()) {
            ScheduleEditorDialog(
                initial = target,
                zones = uiState.zones,
                isNew = false,
                onDismiss = { editingSchedule = null },
                onSave = {
                    onSaveSchedule(it)
                    editingSchedule = null
                }
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: IrrigationScheduleEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_card_${schedule.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = IrrigationFormatters.formatScheduleTime(schedule.hour, schedule.minute),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (schedule.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = schedule.label,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${schedule.zoneName} • ${schedule.durationMinutes} menit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = IrrigationFormatters.formatDaysMask(schedule.daysMask),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (schedule.skipOnRain) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RainSkipBlue.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Umbrella,
                                    contentDescription = "Tunda hujan",
                                    tint = RainSkipBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tunda Hujan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RainSkipBlue
                                )
                            }
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Ubah jadwal")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Hapus jadwal",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleEditorDialog(
    initial: IrrigationScheduleEntity,
    zones: List<IrrigationZoneEntity>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (IrrigationScheduleEntity) -> Unit
) {
    var label by remember { mutableStateOf(initial.label) }
    var selectedZoneId by remember { mutableIntStateOf(initial.zoneId) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }
    var duration by remember { mutableIntStateOf(initial.durationMinutes) }
    var daysMask by remember { mutableIntStateOf(initial.daysMask) }
    var skipOnRain by remember { mutableStateOf(initial.skipOnRain) }

    val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Tambah Jadwal Siram" else "Ubah Jadwal")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nama Jadwal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih Zona:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    zones.forEach { zone ->
                        FilterChip(
                            selected = selectedZoneId == zone.id,
                            onClick = { selectedZoneId = zone.id },
                            label = { Text(zone.name) }
                        )
                    }
                }

                Text(
                    text = "Jam Mulai: ${IrrigationFormatters.formatScheduleTime(hour, minute)}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Jam ($hour)", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = hour.toFloat(),
                            onValueChange = { hour = it.roundToInt().coerceIn(0, 23) },
                            valueRange = 0f..23f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Menit ($minute)", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = (minute / 5).toFloat(),
                            onValueChange = { minute = (it.roundToInt() * 5).coerceIn(0, 55) },
                            valueRange = 0f..11f,
                            steps = 10
                        )
                    }
                }

                Text("Hari Aktif:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayLabels.forEachIndexed { idx, dayName ->
                        val bit = 1 shl idx
                        val selected = (daysMask and bit) != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                daysMask = if (selected) (daysMask and bit.inv()) else (daysMask or bit)
                            },
                            label = { Text(dayName) }
                        )
                    }
                }

                Text(
                    text = "Durasi: $duration menit",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.roundToInt().coerceIn(1, 60) },
                    valueRange = 1f..60f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tunda otomatis jika prakiraan hujan", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = skipOnRain,
                        onCheckedChange = { skipOnRain = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val zone = zones.firstOrNull { it.id == selectedZoneId } ?: zones.first()
                    onSave(
                        initial.copy(
                            zoneId = zone.id,
                            zoneName = zone.name,
                            label = label.trim().ifBlank { "Jadwal Rutin" },
                            hour = hour,
                            minute = minute,
                            durationMinutes = duration,
                            daysMask = if (daysMask == 0) 0b1111111 else daysMask,
                            skipOnRain = skipOnRain
                        )
                    )
                },
                modifier = Modifier.testTag("save_schedule_button")
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
