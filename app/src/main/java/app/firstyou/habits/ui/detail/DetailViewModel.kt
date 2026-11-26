package app.firstyou.habits.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.firstyou.habits.data.HabitKind
import app.firstyou.habits.data.HabitRepository
import app.firstyou.habits.data.Score
import app.firstyou.habits.data.Streak
import app.firstyou.habits.data.StreakRun
import app.firstyou.habits.data.WeekDays
import app.firstyou.habits.data.weekdayRates
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DetailState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val name: String = "",
    val color: Int = 0,
    val typeLabel: String = "",
    val scheduleLabel: String = "",
    val scoreToday: Double = 0.0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val chart: List<Pair<LocalDate, Double>> = emptyList(),
    val runs: List<StreakRun> = emptyList(),
    val weekday: List<Double> = List(7) { 0.0 },
    val done: Set<LocalDate> = emptySet(),
)

class DetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observe(habitId), repository.observeEntries()) { habit, entries ->
                if (habit == null) {
                    DetailState(loading = false, missing = true)
                } else {
                    val today = LocalDate.now()
                    val values = entries[habit.id].orEmpty()
                    val done = values.filter { (_, value) -> habit.countsAsDone(value) }.keys
                    val scheduled = WeekDays.schedule(habit.days)
                    val earliest = done.minOrNull() ?: today.minusWeeks(8)
                    val from = if (earliest.isBefore(today.minusWeeks(8))) earliest else today.minusWeeks(8)
                    val series = Score.series(scheduled, done, from, today)
                    DetailState(
                        loading = false,
                        name = habit.name,
                        color = habit.color,
                        typeLabel = if (habit.kind == HabitKind.MEASURABLE) {
                            val goal = habit.target?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
                            "Medible · ${habit.unit.orEmpty()} · meta $goal"
                        } else {
                            "Sí/No"
                        },
                        scheduleLabel = WeekDays.summary(habit.days),
                        scoreToday = series.lastOrNull()?.second ?: 0.0,
                        currentStreak = Streak.current(scheduled, done, today),
                        bestStreak = Streak.best(scheduled, done, today),
                        chart = Score.weekly(scheduled, done, from, today),
                        runs = Streak.runs(scheduled, done, today),
                        weekday = weekdayRates(scheduled, done, from, today),
                        done = done,
                    )
                }
            }.collect { _state.value = it }
        }
    }

    companion object {
        fun factory(habitId: Long, repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(habitId, repository) as T
            }
        }
    }
}
