package app.firstyou.habits.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.data.Score
import app.firstyou.habits.data.WeekDays
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChartWidget : GlanceAppWidget() {
    // Single dibuja al tamaño mínimo y el launcher estira ese dibujo. Exact usa el tamaño real.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val style = WidgetStyles.chart(context, appWidgetId)
        val repository = (context.applicationContext as HabitsApp).container.repository
        val initialHabit = withContext(Dispatchers.IO) { repository.get(style.habitId) }
        val initialEntries = withContext(Dispatchers.IO) { repository.observeEntries().first() }
        provideContent {
            val tick by WidgetRefresh.tick.collectAsState(initial = 0)
            val habit = initialHabit
            val entries by repository.observeEntries().collectAsState(initial = initialEntries)
            val size = LocalSize.current
            val metrics = LocalContext.current.resources.displayMetrics
            val density = metrics.density
            if (habit == null) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(style.background)))
                        .cornerRadius(style.corners.dp)
                        .clickable(openChartConfig(LocalContext.current, appWidgetId)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Elige un hábito",
                        style = TextStyle(color = ColorProvider(Color(style.text)), fontSize = 16.sp),
                    )
                }
            } else {
                val points = remember(tick, entries, habit.id) {
                    val today = LocalDate.now()
                    val done = entries[habit.id].orEmpty()
                        .filter { (_, value) -> habit.countsAsDone(value) }
                        .keys
                    val earliest = done.minOrNull() ?: today.minusWeeks(8)
                    val from = if (earliest.isBefore(today.minusWeeks(8))) earliest else today.minusWeeks(8)
                    Score.weekly(WeekDays.schedule(habit.days), done, from, today)
                }
                val bitmap = remember(size, points, style, habit.color, habit.name) {
                    renderScoreChart(
                        title = habit.name,
                        points = points,
                        widthPx = (size.width.value * density).toInt().coerceAtLeast(1),
                        heightPx = (size.height.value * density).toInt().coerceAtLeast(1),
                        density = density,
                        densityDpi = metrics.densityDpi,
                        background = style.background,
                        textColor = style.text,
                        lineColor = if (style.line == 0) habit.color else style.line,
                        showTitle = style.showTitle,
                        showAxis = style.showAxis,
                        titleSizeSp = style.titleSize,
                    )
                }
                Box(modifier = GlanceModifier.fillMaxSize()) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = habit.name,
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(style.corners.dp),
                        contentScale = ContentScale.FillBounds,
                    )
                    Text(
                        text = "Editar",
                        style = TextStyle(
                            color = ColorProvider(Color(style.text).copy(alpha = 0.55f)),
                            fontSize = 11.sp,
                        ),
                        modifier = GlanceModifier
                            .padding(8.dp)
                            .clickable(openChartConfig(LocalContext.current, appWidgetId)),
                    )
                }
            }
        }
    }
}

class ChartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChartWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetStyles.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

private fun openChartConfig(context: Context, appWidgetId: Int) = actionStartActivity(
    Intent(context, ChartWidgetConfigActivity::class.java).apply {
        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    },
)
