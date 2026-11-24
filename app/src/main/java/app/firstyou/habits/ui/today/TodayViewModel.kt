package app.firstyou.habits.ui.today

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

data class TodayItem(
    val id: Long,
    val name: String,
    val color: Int,
    val daysLabel: String,
    val doneToday: Boolean,
    val streak: Int,
)

data class TodayState(
    val loading: Boolean = true,
    val hasHabits: Boolean = false,
    val items: List<TodayItem> = emptyList(),
)

class TodayViewModel(private val repository: HabitRepository) : ViewModel() {
    private val _state = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeActive(), repository.observeCompletions()) { habits, done ->
                val today = LocalDate.now()
                TodayState(
                    loading = false,
                    hasHabits = habits.isNotEmpty(),
                    items = habits
                        .filter { WeekDays.isScheduled(it.days, today) }
                        .map { habit ->
                            val dates = done[habit.id].orEmpty()
                            TodayItem(
                                id = habit.id,
                                name = habit.name,
                                color = habit.color,
                                daysLabel = WeekDays.summary(habit.days),
                                doneToday = today in dates,
                                streak = Streak.current(WeekDays.schedule(habit.days), dates, today),
                            )
                        },
                )
            }.collect { _state.value = it }
        }
    }

    fun toggleToday(id: Long, done: Boolean) {
        viewModelScope.launch {
            repository.setDone(id, LocalDate.now(), done)
        }
    }

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodayViewModel(repository) as T
            }
        }
    }
}
