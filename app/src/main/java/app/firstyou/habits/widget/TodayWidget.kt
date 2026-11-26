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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.MainActivity
import app.firstyou.habits.data.HabitKind
import app.firstyou.habits.data.widgetRows
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val HabitIdKey = ActionParameters.Key<Long>("habitId")

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val style = WidgetStyles.list(context, appWidgetId)
        val repository = (context.applicationContext as HabitsApp).container.repository
        val initialHabits = withContext(Dispatchers.IO) { repository.observeActive().first() }
        val initialEntries = withContext(Dispatchers.IO) { repository.observeEntries().first() }
        provideContent {
            val tick by WidgetRefresh.tick.collectAsState(initial = 0)
            val habits by repository.observeActive().collectAsState(initial = initialHabits)
            val entries by repository.observeEntries().collectAsState(initial = initialEntries)
            val today = remember(tick, habits, entries) { LocalDate.now() }
            val rows = remember(tick, habits, entries, style, today) {
                widgetRows(habits, entries, today, limit = style.maxRows, todayOnly = style.todayOnly)
            }
            val metrics = LocalContext.current.resources.displayMetrics
            ListWidgetContent(LocalContext.current, appWidgetId, style, rows, metrics.density, metrics.densityDpi)
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetStyles.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class ToggleTodayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val repository = (context.applicationContext as HabitsApp).container.repository
        val habit = repository.get(habitId) ?: return
        val today = LocalDate.now()
        val value = repository.entry(habitId, today)
        if (habit.countsAsDone(value)) {
            repository.setEntry(habitId, today, null)
        } else if (habit.kind == HabitKind.MEASURABLE) {
            repository.setEntry(habitId, today, habit.target ?: 1.0)
        } else {
            repository.setEntry(habitId, today, 1.0)
        }
    }
}

suspend fun refreshTodayWidgets(context: Context) {
    WidgetRefresh.bump()
    TodayWidget().updateAll(context)
    ChartWidget().updateAll(context)
}

@androidx.compose.runtime.Composable
private fun ListWidgetContent(
    context: Context,
    appWidgetId: Int,
    style: ListWidgetStyle,
    rows: List<app.firstyou.habits.data.WidgetRow>,
    density: Float,
    densityDpi: Int,
) {
    val textColor = ColorProvider(Color(style.text))
    val weight = if (style.bold) FontWeight.Bold else FontWeight.Normal
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(style.background)))
            .cornerRadius(style.corners.dp)
            .padding(style.padding.dp),
    ) {
        if (style.showTitle) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "First Habits",
                    style = TextStyle(
                        color = textColor,
                        fontSize = style.fontSize.sp,
                        fontWeight = weight,
                    ),
                    modifier = GlanceModifier.clickable(openApp(context)),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(
                    text = "Editar",
                    style = TextStyle(
                        color = ColorProvider(Color(style.text).copy(alpha = 0.55f)),
                        fontSize = 11.sp,
                    ),
                    modifier = GlanceModifier.clickable(openListConfig(context, appWidgetId)),
                )
            }
        }
        if (rows.isEmpty()) {
            Spacer(modifier = GlanceModifier.padding(top = 8.dp))
            Text(
                text = if (style.todayOnly) "Nada para hoy" else "No hay hábitos",
                style = TextStyle(color = textColor, fontSize = style.fontSize.sp),
            )
        } else {
            rows.forEach { row ->
                val nameColor = if (style.useHabitColor) ColorProvider(Color(row.color)) else textColor
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable(
                            actionRunCallback<ToggleTodayAction>(
                                actionParametersOf(HabitIdKey to row.id),
                            ),
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val box = remember(row.done, row.color, densityDpi) {
                        habitCheckBitmap(
                            done = row.done,
                            accent = row.color,
                            sizePx = (22f * density).toInt().coerceAtLeast(1),
                            densityDpi = densityDpi,
                        )
                    }
                    Image(
                        provider = ImageProvider(box),
                        contentDescription = if (row.done) "Hecho" else "Pendiente",
                        modifier = GlanceModifier.size(22.dp),
                    )
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Text(
                        text = row.name,
                        maxLines = 1,
                        style = TextStyle(
                            color = nameColor,
                            fontSize = style.fontSize.sp,
                            fontWeight = weight,
                        ),
                    )
                    val amount = row.mark.takeIf { it.any(Char::isDigit) }
                    if (amount != null) {
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = amount,
                            style = TextStyle(
                                color = if (row.done) {
                                    ColorProvider(Color(row.color))
                                } else {
                                    ColorProvider(Color(style.text).copy(alpha = 0.45f))
                                },
                                fontSize = (style.fontSize - 1).sp,
                                fontWeight = weight,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun openApp(context: Context) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    },
)

private fun openListConfig(context: Context, appWidgetId: Int) = actionStartActivity(
    Intent(context, ListWidgetConfigActivity::class.java).apply {
        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    },
)
