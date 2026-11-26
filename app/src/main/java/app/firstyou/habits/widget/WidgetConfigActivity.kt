package app.firstyou.habits.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.data.Habit
import app.firstyou.habits.ui.theme.FirstHabitsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ListWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = widgetIdOrNull() ?: return
        val initial = WidgetStyles.list(this, appWidgetId)
        setContent {
            var style by remember { mutableStateOf(initial) }
            ConfigScaffold(title = "Lista de hábitos", onCancel = { finishCanceled(appWidgetId) }) {
                Text("El tamaño del recuadro se cambia estirándolo en la pantalla principal.", color = ConfigInk)
                Spacer(Modifier.size(12.dp))
                Text("Fondo", color = ConfigInk, fontWeight = FontWeight.SemiBold)
                ColorDots(WidgetStyles.backgrounds, style.background) { style = style.copy(background = it) }
                Text("Texto", color = ConfigInk, fontWeight = FontWeight.SemiBold)
                ColorDots(WidgetStyles.inks, style.text) { style = style.copy(text = it) }
                ChoiceRow("Nombres") {
                    Choice("Color de cada uno", style.useHabitColor) { style = style.copy(useHabitColor = true) }
                    Choice("Un solo color", !style.useHabitColor) { style = style.copy(useHabitColor = false) }
                }
                ChoiceRow("Letra") {
                    listOf(13, 16, 20, 24).forEach { size ->
                        Choice(size.toString(), style.fontSize == size) { style = style.copy(fontSize = size) }
                    }
                }
                ChoiceRow("Grosor") {
                    Choice("Normal", !style.bold) { style = style.copy(bold = false) }
                    Choice("Negrita", style.bold) { style = style.copy(bold = true) }
                }
                ChoiceRow("Esquinas") {
                    listOf(0, 12, 24, 32).forEach { value ->
                        Choice(if (value == 0) "Recta" else value.toString(), style.corners == value) {
                            style = style.copy(corners = value)
                        }
                    }
                }
                ChoiceRow("Espacio") {
                    listOf(8, 14, 20).forEach { value ->
                        Choice(value.toString(), style.padding == value) { style = style.copy(padding = value) }
                    }
                }
                ChoiceRow("Muestra") {
                    Choice("Los de hoy", style.todayOnly) { style = style.copy(todayOnly = true) }
                    Choice("Todos", !style.todayOnly) { style = style.copy(todayOnly = false) }
                }
                ChoiceRow("Cantidad") {
                    listOf(4, 8, 12).forEach { value ->
                        Choice(value.toString(), style.maxRows == value) { style = style.copy(maxRows = value) }
                    }
                }
                ChoiceRow("Título") {
                    Choice("Sí", style.showTitle) { style = style.copy(showTitle = true) }
                    Choice("No", !style.showTitle) { style = style.copy(showTitle = false) }
                }
                Button(
                    onClick = { saveAndClose(appWidgetId, TodayWidget()) { WidgetStyles.saveList(this, appWidgetId, style) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar") }
            }
        }
    }
}

class ChartWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = widgetIdOrNull() ?: return
        val initial = WidgetStyles.chart(this, appWidgetId)
        val app = application as HabitsApp
        setContent {
            var style by remember { mutableStateOf(initial) }
            var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
            LaunchedEffect(Unit) {
                habits = app.container.repository.observeActive().first()
                if (style.habitId == 0L) {
                    habits.firstOrNull()?.let { style = style.copy(habitId = it.id) }
                }
            }
            ConfigScaffold(title = "Gráfica de hábito", onCancel = { finishCanceled(appWidgetId) }) {
                Text("Hábito", color = ConfigInk, fontWeight = FontWeight.SemiBold)
                if (habits.isEmpty()) {
                    Text("Crea un hábito en la app antes de poner la gráfica.", color = ConfigInk)
                } else {
                    FlowChoices {
                        habits.forEach { habit ->
                            Choice(habit.name, style.habitId == habit.id) {
                                style = style.copy(habitId = habit.id)
                            }
                        }
                    }
                }
                Text("Fondo", color = ConfigInk, fontWeight = FontWeight.SemiBold)
                ColorDots(WidgetStyles.backgrounds, style.background) { style = style.copy(background = it) }
                Text("Texto", color = ConfigInk, fontWeight = FontWeight.SemiBold)
                ColorDots(WidgetStyles.inks, style.text) { style = style.copy(text = it) }
                Text("Línea", color = ConfigInk, fontWeight = FontWeight.SemiBold)
                FlowChoices {
                    Choice("Color del hábito", style.line == 0) { style = style.copy(line = 0) }
                }
                ColorDots(WidgetStyles.inks + WidgetStyles.backgrounds, style.line) { style = style.copy(line = it) }
                ChoiceRow("Título") {
                    Choice("Sí", style.showTitle) { style = style.copy(showTitle = true) }
                    Choice("No", !style.showTitle) { style = style.copy(showTitle = false) }
                }
                ChoiceRow("Eje y fechas") {
                    Choice("Sí", style.showAxis) { style = style.copy(showAxis = true) }
                    Choice("No", !style.showAxis) { style = style.copy(showAxis = false) }
                }
                ChoiceRow("Esquinas") {
                    listOf(0, 12, 24, 32).forEach { value ->
                        Choice(if (value == 0) "Recta" else value.toString(), style.corners == value) {
                            style = style.copy(corners = value)
                        }
                    }
                }
                ChoiceRow("Tamaño del título") {
                    listOf(18, 22, 28).forEach { value ->
                        Choice(value.toString(), style.titleSize == value) { style = style.copy(titleSize = value) }
                    }
                }
                Button(
                    onClick = {
                        if (style.habitId != 0L) {
                            saveAndClose(appWidgetId, ChartWidget()) {
                                WidgetStyles.saveChart(this, appWidgetId, style)
                            }
                        }
                    },
                    enabled = style.habitId != 0L && habits.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar") }
            }
        }
    }
}

private fun ComponentActivity.widgetIdOrNull(): Int? {
    val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        ?: AppWidgetManager.INVALID_APPWIDGET_ID
    if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
        finish()
        return null
    }
    setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
    return id
}

private fun ComponentActivity.finishCanceled(appWidgetId: Int) {
    setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
    finish()
}

private fun ComponentActivity.saveAndClose(appWidgetId: Int, widget: GlanceAppWidget, save: () -> Unit) {
    save()
    lifecycleScope.launch {
        val glanceId = try {
            GlanceAppWidgetManager(this@saveAndClose).getGlanceIdBy(appWidgetId)
        } catch (_: Exception) {
            null
        }
        if (glanceId != null) widget.update(this@saveAndClose, glanceId)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

private val ConfigInk = Color(0xFFF2F2F2)
private val ConfigCard = Color(0xFF2A2A2A)

@Composable
private fun ConfigScaffold(title: String, onCancel: () -> Unit, content: @Composable () -> Unit) {
    FirstHabitsTheme(dark = true, accent = Color(0xFFD0BCFF)) {
        CompositionLocalProvider(LocalContentColor provides ConfigInk) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFF121212))
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = ConfigInk),
                ) { Text("Cancelar") }
                Text(
                    text = title,
                    color = ConfigInk,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(12.dp))
                content()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorDots(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        colors.distinct().forEach { color ->
            Spacer(
                Modifier
                    .size(32.dp)
                    .background(Color(color), CircleShape)
                    .border(
                        width = if (color == selected) 3.dp else 1.dp,
                        color = if (color == selected) MaterialTheme.colorScheme.onSurface else Color(0x33000000),
                        shape = CircleShape,
                    )
                    .clickable { onPick(color) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(label: String, content: @Composable () -> Unit) {
    Text(label, color = ConfigInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    FlowChoices { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChoices(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = ConfigCard,
            labelColor = ConfigInk,
            selectedContainerColor = Color(0xFFD0BCFF),
            selectedLabelColor = Color(0xFF1C1B1A),
        ),
    )
}
