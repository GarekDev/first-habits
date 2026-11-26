package app.firstyou.habits.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.data.AppSettings
import app.firstyou.habits.data.HabitColors
import app.firstyou.habits.ui.backup.BackupControls
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HabitsApp
    val settings = app.container.settings
    val dark by settings.dark.collectAsState(initial = true)
    val accent by settings.accent.collectAsState(initial = AppSettings.DEFAULT_ACCENT)
    val scope = rememberCoroutineScope()
    val start = remember {
        FloatArray(3).also { AndroidColor.colorToHSV(AppSettings.DEFAULT_ACCENT, it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1].coerceAtLeast(0.15f)) }
    var value by remember { mutableFloatStateOf(start[2].coerceIn(0.35f, 1f)) }
    LaunchedEffect(Unit) {
        val stored = settings.accent.first()
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(stored, hsv)
        hue = hsv[0]
        saturation = hsv[1].coerceIn(0.15f, 1f)
        value = hsv[2].coerceIn(0.35f, 1f)
    }

    fun publish() {
        val color = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        scope.launch { settings.setAccent(color) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(20.dp))
        Text(text = "Tema", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dark,
                onClick = { scope.launch { settings.setDark(true) } },
                label = { Text("Oscuro") },
            )
            FilterChip(
                selected = !dark,
                onClick = { scope.launch { settings.setDark(false) } },
                label = { Text("Claro") },
            )
        }
        Spacer(Modifier.size(24.dp))
        Text(text = "Color de acento", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        Spacer(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(accent)),
        )
        Spacer(Modifier.size(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HabitColors.all.forEach { color ->
                Spacer(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (color == accent) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(color, hsv)
                            hue = hsv[0]
                            saturation = hsv[1].coerceIn(0.15f, 1f)
                            value = hsv[2].coerceIn(0.35f, 1f)
                            scope.launch { settings.setAccent(color) }
                        },
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        Text("Tono")
        Slider(value = hue, onValueChange = { hue = it; publish() }, valueRange = 0f..360f, modifier = Modifier.fillMaxWidth())
        Text("Intensidad")
        Slider(
            value = saturation,
            onValueChange = { saturation = it; publish() },
            valueRange = 0.15f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Brillo")
        Slider(
            value = value,
            onValueChange = { value = it; publish() },
            valueRange = 0.35f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(28.dp))
        Text(text = "Copia de seguridad", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        BackupControls()
    }
}
