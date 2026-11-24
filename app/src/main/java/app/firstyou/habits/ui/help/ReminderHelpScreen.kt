package app.firstyou.habits.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.firstyou.habits.reminder.HelpOpen
import app.firstyou.habits.reminder.ReminderHelp

@Composable
fun ReminderHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(ReminderHelp.status(context)) }
    var alarmNote by remember { mutableStateOf<String?>(null) }
    var autoNote by remember { mutableStateOf<String?>(null) }
    var protectedNote by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = ReminderHelp.status(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text(
            text = "Para que suene el recordatorio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Los nombres de estos menús cambian entre versiones de XOS. El buscador de Ajustes es el camino seguro.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(8.dp))
        SetupRow(
            title = "Notificaciones",
            body = "Ajustes, notificaciones, First Habits. ${status.notifications}",
            action = "Abrir",
            onOpen = { ReminderHelp.openNotifications(context) },
        )
        SetupRow(
            title = "Alarmas y recordatorios",
            body = "Ajustes, alarmas y recordatorios, o Acceso especial de apps. ${status.alarms}",
            action = if (status.alarmsAction) "Abrir" else null,
            note = alarmNote,
            onOpen = {
                alarmNote = if (ReminderHelp.openAlarms(context)) {
                    null
                } else {
                    "No pude abrir esa pantalla. Búscala en Ajustes."
                }
            },
        )
        SetupRow(
            title = "Batería de esta app",
            body = "En la ficha de First Habits, Batería: permite actividad en segundo plano y desactiva «Detener la app cuando esté inactiva», o el texto equivalente. ${status.battery}",
            action = "Abrir ficha",
            onOpen = { ReminderHelp.openAppDetails(context) },
        )
        SetupRow(
            title = "Inicio automático",
            body = "Si tu Infinix tiene Phone Master o XManager, activa ahí First Habits. Si no tienes esa app, este paso puede no existir.",
            action = "Abrir",
            note = autoNote,
            onOpen = {
                autoNote = noteFor(ReminderHelp.openAutoStart(context), "inicio automático")
            },
        )
        SetupRow(
            title = "Aplicaciones protegidas",
            body = "Si tienes Phone Master, añade First Habits a las protegidas. Si no tienes esa app, este paso puede no existir.",
            action = "Abrir",
            note = protectedNote,
            onOpen = {
                protectedNote = noteFor(ReminderHelp.openPhoneMaster(context), "protegidas")
            },
        )
        SetupRow(
            title = "Recientes",
            body = "Abre First Habits, entra en Recientes y toca el candado de su tarjeta. Una limpieza de memoria no debe cerrarla. La app no puede saber si ya está.",
            action = null,
            onOpen = {},
        )
    }
}

private fun noteFor(result: HelpOpen, search: String): String? {
    return when (result) {
        HelpOpen.Target -> null
        HelpOpen.Settings -> "No tienes Phone Master. En el buscador de Ajustes escribe «$search». Si no sale nada, sáltalo."
        HelpOpen.Failed -> "No pude abrir Ajustes. Búscala tú: $search."
    }
}

@Composable
private fun SetupRow(
    title: String,
    body: String,
    action: String?,
    onOpen: () -> Unit,
    note: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            TextButton(onClick = onOpen) { Text(action) }
        }
        if (note != null) {
            Text(text = note, color = MaterialTheme.colorScheme.error)
        }
        HorizontalDivider()
    }
}
