package app.firstyou.habits.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.data.StreakRun
import app.firstyou.habits.data.WeekDays
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun onHabitColor(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.62f) Color.Black else Color.White
}

@Composable
fun DetailScreen(
    habitId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HabitsApp
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.factory(habitId, app.container.repository),
    )
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Volver") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text("Editar") }
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.missing -> Text("Ese hábito ya no está.")
            else -> {
                Text(
                    text = state.name,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(state.color),
                )
                Spacer(Modifier.size(16.dp))
                SummaryPage(state)
                SectionBreak()
                SectionTitle("Puntuación")
                ScorePage(state)
                SectionBreak()
                SectionTitle("Calendario")
                CalendarPage(state)
                SectionBreak()
                SectionTitle("Mejores rachas")
                StreaksPage(state.runs)
                SectionBreak()
                SectionTitle("Frecuencia")
                FrequencyPage(state.weekday, Color(state.color))
            }
        }
    }
}

@Composable
private fun SectionBreak() {
    Spacer(Modifier.size(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    Spacer(Modifier.size(20.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.size(8.dp))
}

@Composable
private fun SummaryPage(state: DetailState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Stat("${(state.scoreToday * 100).toInt()}", "Puntuación")
        Stat(state.currentStreak.toString(), "Racha")
        Stat(state.bestStreak.toString(), "Mejor")
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScorePage(state: DetailState) {
    val points = state.chart
    val line = Color(state.color)
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = state.name,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (points.size < 2) {
                Text("Todavía no hay suficientes días para la gráfica.")
            } else {
                Row(Modifier.fillMaxWidth().height(220.dp)) {
                    Column(
                        modifier = Modifier.width(32.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf("100", "80", "60", "40", "20").forEach { label ->
                            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Canvas(Modifier.weight(1f).fillMaxSize()) {
                        val left = 8.dp.toPx()
                        val right = size.width - 8.dp.toPx()
                        val top = 8.dp.toPx()
                        val bottom = size.height - 8.dp.toPx()
                        for (step in 1..5) {
                            val y = bottom - (bottom - top) * (step * 20 / 100f)
                            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                        }
                        fun xOf(index: Int) = left + (right - left) * index / (points.size - 1)
                        fun yOf(score: Double): Float {
                            val clamped = score.coerceIn(0.0, 1.0)
                            return (bottom - (bottom - top) * clamped).toFloat()
                        }
                        for (index in 0 until points.lastIndex) {
                            drawLine(
                                color = line,
                                start = Offset(xOf(index), yOf(points[index].second)),
                                end = Offset(xOf(index + 1), yOf(points[index + 1].second)),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                        points.forEachIndexed { index, point ->
                            drawCircle(line, radius = 4.dp.toPx(), center = Offset(xOf(index), yOf(point.second)))
                        }
                    }
                }
                ChartDates(points)
            }
        }
    }
}

@Composable
private fun ChartDates(points: List<Pair<LocalDate, Double>>) {
    val step = (points.size / 6).coerceAtLeast(1)
    val locale = Locale.forLanguageTag("es")
    val month = DateTimeFormatter.ofPattern("MMM", locale)
    Row(Modifier.fillMaxWidth().padding(start = 32.dp, top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        points.forEachIndexed { index, (date, _) ->
            if (index % step == 0 || index == points.lastIndex) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.format(month), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(date.dayOfMonth.toString(), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CalendarPage(state: DetailState) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val locale = Locale.forLanguageTag("es")
    val title = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("Anterior") }
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("Siguiente") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WeekDays.labels.forEach { label ->
                Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value - 1
        val cells = lead + month.lengthOfMonth()
        val rows = (cells + 6) / 7
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (column in 0 until 7) {
                    val index = row * 7 + column
                    val day = index - lead + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val done = date in state.done
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (done) Color(state.color) else Color.Transparent,
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = if (done) onHabitColor(Color(state.color)) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreaksPage(runs: List<StreakRun>) {
    if (runs.isEmpty()) {
        Text("Todavía no hay rachas.")
        return
    }
    val locale = Locale.forLanguageTag("es")
    val format = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    runs.forEach { run ->
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("${run.days} días", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(
                "${run.start.format(format)} – ${run.end.format(format)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FrequencyPage(rates: List<Double>, color: Color) {
    WeekDays.labels.forEachIndexed { index, label ->
        val rate = rates.getOrElse(index) { 0.0 }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.width(40.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(7.dp)),
            ) {
                if (rate > 0.0) {
                    Box(
                        Modifier
                            .fillMaxWidth(rate.toFloat().coerceIn(0.02f, 1f))
                            .height(14.dp)
                            .background(color, RoundedCornerShape(7.dp)),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${(rate * 100).toInt()}%", modifier = Modifier.width(40.dp))
        }
    }
}
