package app.firstyou.habits.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.firstyou.habits.HabitsApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    onNewHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenReminderHelp: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HabitsApp
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(app.container.repository))
    val state by viewModel.state.collectAsState()
    val todayLabel = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es")),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hoy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onOpenArchived) {
                Text("Archivados")
            }
        }
        Text(
            text = todayLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenReminderHelp) {
            Text("Para que suene el recordatorio")
        }
        TextButton(onClick = onOpenBackup) {
            Text("Copia de seguridad")
        }
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> CircularProgressIndicator()
                !state.hasHabits -> Text("Todavía no hay hábitos.")
                state.items.isEmpty() -> Text("Hoy no toca ninguno.")
                else -> {
                    LazyColumn {
                        items(state.items, key = { it.id }) { habit ->
                            HabitRow(
                                habit = habit,
                                onOpen = { onOpenHabit(habit.id) },
                                onToggle = { viewModel.toggleToday(habit.id, !habit.doneToday) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = onNewHabit, modifier = Modifier.fillMaxWidth()) {
            Text("Nuevo hábito")
        }
    }
}

@Composable
private fun HabitRow(
    habit: TodayItem,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = Color(habit.color),
        ) {}
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen),
        ) {
            Text(text = habit.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${habit.daysLabel}  ·  Racha ${habit.streak}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DoneMark(done = habit.doneToday, color = Color(habit.color), onToggle = onToggle)
    }
}

@Composable
private fun DoneMark(done: Boolean, color: Color, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onToggle),
        shape = CircleShape,
        color = if (done) color else Color.Transparent,
        border = if (done) null else BorderStroke(2.dp, color),
    ) {}
}
