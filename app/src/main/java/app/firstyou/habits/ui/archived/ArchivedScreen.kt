package app.firstyou.habits.ui.archived

import androidx.compose.foundation.layout.Arrangement
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
import app.firstyou.habits.data.Habit

@Composable
fun ArchivedScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HabitsApp
    val viewModel: ArchivedViewModel = viewModel(factory = ArchivedViewModel.factory(app.container.repository))
    val habits by viewModel.habits.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Volver")
        }
        Text(
            text = "Archivados",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(16.dp))
        when (val current = habits) {
            null -> CircularProgressIndicator()
            emptyList<Habit>() -> Text("No hay hábitos archivados.")
            else -> {
                LazyColumn {
                    items(current, key = { it.id }) { habit ->
                        ArchivedRow(habit = habit, onRestore = { viewModel.restore(habit.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedRow(habit: Habit, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = Color(habit.color),
        ) {}
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = habit.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRestore) {
            Text("Restaurar")
        }
    }
}
