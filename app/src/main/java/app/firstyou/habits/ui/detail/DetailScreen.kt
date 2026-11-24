package app.firstyou.habits.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.firstyou.habits.HabitsApp

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
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Volver")
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.missing -> Text("Ese hábito ya no está.")
            else -> {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Racha ${state.currentStreak}  ·  Mejor ${state.bestStreak}")
                Spacer(modifier = Modifier.size(16.dp))
                state.days.forEach { day ->
                    DayRow(
                        label = day.label,
                        scheduled = day.scheduled,
                        done = day.done,
                        color = Color(state.color),
                        onToggle = { viewModel.toggle(day.date, !day.done) },
                    )
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.size(20.dp))
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar")
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    label: String,
    scheduled: Boolean,
    done: Boolean,
    color: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (scheduled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
        if (scheduled) {
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onToggle),
                shape = CircleShape,
                color = if (done) color else Color.Transparent,
                border = if (done) null else BorderStroke(2.dp, color),
            ) {}
        } else {
            Text(
                text = "No toca",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}
