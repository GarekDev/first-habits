package app.firstyou.habits.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.firstyou.habits.data.HabitRepository
import app.firstyou.habits.data.Streak
import app.firstyou.habits.data.WeekDays
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DetailDay(
    val date: LocalDate,
    val label: String,
    val scheduled: Boolean,
    val done: Boolean,
)

data class DetailState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val name: String = "",
    val color: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val days: List<DetailDay> = emptyList(),
)

class DetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observe(habitId), repository.observeCompletions()) { habit, done ->
                if (habit == null) {
                    DetailState(loading = false, missing = true)
                } else {
                    val today = LocalDate.now()
                    val dates = done[habit.id].orEmpty()
                    val scheduled = WeekDays.schedule(habit.days)
                    DetailState(
                        loading = false,
                        name = habit.name,
                        color = habit.color,
                        currentStreak = Streak.current(scheduled, dates, today),
                        bestStreak = Streak.best(scheduled, dates, today),
                        days = (6 downTo 0).map { ago ->
                            val date = today.minusDays(ago.toLong())
                            DetailDay(
                                date = date,
                                label = dayLabel(date, today),
                                scheduled = scheduled(date),
                                done = date in dates,
                            )
                        },
                    )
                }
            }.collect { _state.value = it }
        }
    }

    fun toggle(date: LocalDate, done: Boolean) {
        viewModelScope.launch {
            repository.setDone(habitId, date, done)
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

private fun dayLabel(date: LocalDate, today: LocalDate): String {
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d", java.util.Locale.forLanguageTag("es")))
    }
}
