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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.MainActivity
import app.firstyou.habits.data.widgetRows
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val HabitIdKey = ActionParameters.Key<Long>("habitId")

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as HabitsApp).container.repository
        val initialHabits = withContext(Dispatchers.IO) { repository.observeActive().first() }
        val initialCompletions = withContext(Dispatchers.IO) { repository.observeCompletions().first() }
        provideContent {
            val tick by refresh.collectAsState(initial = 0)
            val habits by repository.observeActive().collectAsState(initial = initialHabits)
            val completions by repository.observeCompletions().collectAsState(initial = initialCompletions)
            val today = remember(tick, habits, completions) { LocalDate.now() }
            TodayWidgetContent(context, widgetRows(habits, completions, today))
        }
    }

    companion object {
        private val refresh = MutableStateFlow(0)

        fun bump() {
            refresh.value += 1
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class ToggleTodayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val repository = (context.applicationContext as HabitsApp).container.repository
        val today = LocalDate.now()
        val done = today in repository.completedDays(habitId)
        repository.setDone(habitId, today, !done)
    }
}

suspend fun refreshTodayWidgets(context: Context) {
    TodayWidget.bump()
    TodayWidget().updateAll(context)
}

@androidx.compose.runtime.Composable
private fun TodayWidgetContent(context: Context, rows: List<app.firstyou.habits.data.WidgetRow>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF7F4EF)))
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Text(
            text = "First Habits",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ColorProvider(Color(0xFF1C1B1A)),
            ),
            modifier = GlanceModifier.clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                ),
            ),
        )
        if (rows.isEmpty()) {
            Spacer(modifier = GlanceModifier.padding(top = 8.dp))
            Text(
                text = "Nada para hoy",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(Color(0xFF5C5854)),
                ),
            )
        } else {
            rows.forEach { row ->
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
                    Text(
                        text = if (row.done) "●" else "○",
                        style = TextStyle(
                            color = ColorProvider(Color(row.color)),
                            fontSize = 16.sp,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = row.name,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(Color(0xFF1C1B1A)),
                        ),
                    )
                }
            }
        }
    }
}
