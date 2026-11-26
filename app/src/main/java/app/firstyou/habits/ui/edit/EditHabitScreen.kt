package app.firstyou.habits.ui.edit

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.data.HabitColors
import app.firstyou.habits.data.HabitKind
import app.firstyou.habits.data.ReminderTime
import app.firstyou.habits.data.WeekDays

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditHabitScreen(
    habitId: Long,
    onDone: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HabitsApp
    val context = LocalContext.current
    val viewModel: EditHabitViewModel = viewModel(
        factory = EditHabitViewModel.factory(habitId, app.container.repository),
    )
    val state by viewModel.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val askNotification = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.save {
            openExactAlarmSettingsIfNeeded(context)
            onDone()
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Borrar hábito") },
            text = { Text("Esto borra el hábito. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDone) }) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onDone) {
            Text("Volver")
        }
        Text(
            text = if (state.isNew) "Nuevo hábito" else "Editar hábito",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(16.dp))
        when {
            state.loading -> CircularProgressIndicator()
            state.missing -> Text("Ese hábito ya no está.")
            else -> EditHabitForm(
                state = state,
                onName = viewModel::onName,
                onKind = viewModel::onKind,
                onUnit = viewModel::onUnit,
                onTarget = viewModel::onTarget,
                onColor = viewModel::onColor,
                onToggleDay = viewModel::onToggleDay,
                onReminder = viewModel::onReminder,
                onSave = {
                    val wantsReminder = viewModel.state.value.reminderMinutes != null
                    val needsNotification = wantsReminder &&
                        Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (needsNotification) {
                        askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.save {
                            if (viewModel.state.value.reminderMinutes != null) {
                                openExactAlarmSettingsIfNeeded(context)
                            }
                            onDone()
                        }
                    }
                },
                onArchive = { viewModel.archive(onDone) },
                onAskDelete = { confirmDelete = true },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditHabitForm(
    state: EditHabitState,
    onName: (String) -> Unit,
    onKind: (HabitKind) -> Unit,
    onUnit: (String) -> Unit,
    onTarget: (String) -> Unit,
    onColor: (Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onReminder: (Int?) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onAskDelete: () -> Unit,
) {
    OutlinedTextField(
        value = state.name,
        onValueChange = onName,
        label = { Text("Nombre") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.size(20.dp))
    Text(text = "Tipo", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.kind == HabitKind.YES_NO,
            onClick = { onKind(HabitKind.YES_NO) },
            label = { Text("Sí/No") },
        )
        FilterChip(
            selected = state.kind == HabitKind.MEASURABLE,
            onClick = { onKind(HabitKind.MEASURABLE) },
            label = { Text("Medible") },
        )
    }
    if (state.kind == HabitKind.MEASURABLE) {
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = state.unit,
            onValueChange = onUnit,
            label = { Text("Unidad") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = state.targetText,
            onValueChange = onTarget,
            label = { Text("Meta del día") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (state.error != null) {
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = state.error, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.size(20.dp))
    Text(text = "Color", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HabitColors.all.forEach { color ->
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onColor(color) },
                shape = CircleShape,
                color = Color(color),
                border = if (color == state.color) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                } else {
                    null
                },
            ) {}
        }
    }
    Spacer(modifier = Modifier.size(20.dp))
    Text(text = "Días", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WeekDays.labels.forEachIndexed { index, label ->
            FilterChip(
                selected = WeekDays.has(state.days, index),
                onClick = { onToggleDay(index) },
                label = { Text(label) },
            )
        }
    }
    Spacer(modifier = Modifier.size(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Recordatorio", style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = state.reminderMinutes != null,
            onCheckedChange = { enabled ->
                onReminder(if (enabled) state.reminderMinutes ?: 8 * 60 else null)
            },
        )
    }
    if (state.reminderMinutes != null) {
        var showTime by remember { mutableStateOf(false) }
        val minutes = state.reminderMinutes
        TextButton(onClick = { showTime = true }) {
            Text(ReminderTime.format(minutes))
        }
        if (showTime) {
            val picker = rememberTimePickerState(
                initialHour = minutes / 60,
                initialMinute = minutes % 60,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = { showTime = false },
                confirmButton = {
                    TextButton(onClick = {
                        onReminder(picker.hour * 60 + picker.minute)
                        showTime = false
                    }) { Text("Listo") }
                },
                dismissButton = {
                    TextButton(onClick = { showTime = false }) { Text("Cancelar") }
                },
                text = { TimePicker(state = picker) },
            )
        }
    }
    Spacer(modifier = Modifier.size(24.dp))
    Button(
        onClick = onSave,
        enabled = state.name.isNotBlank() && !state.saving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.saving) "Guardando" else "Guardar")
    }
    if (!state.isNew) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onArchive) {
                Text("Archivar")
            }
            TextButton(onClick = onAskDelete) {
                Text("Borrar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun openExactAlarmSettingsIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val alarms = context.getSystemService(AlarmManager::class.java)
    if (alarms.canScheduleExactAlarms()) return
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (_: android.content.ActivityNotFoundException) {
    }
}
