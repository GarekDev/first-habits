package app.firstyou.habits.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.firstyou.habits.data.HabitKind
import app.firstyou.habits.data.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class BoardHabit(
    val id: Long,
    val name: String,
    val color: Int,
    val kind: HabitKind,
    val unit: String?,
    val target: Double?,
    val days: Int,
    val values: Map<LocalDate, Double>,
) {
    fun met(value: Double?): Boolean {
        if (value == null) return false
        return when (kind) {
            HabitKind.YES_NO -> value > 0.0
            HabitKind.MEASURABLE -> value + 0.000001 >= (target ?: 1.0)
        }
    }
}

enum class BoardSort { MANUAL, NAME }

data class BoardState(
    val loading: Boolean = true,
    val habits: List<BoardHabit> = emptyList(),
    val sort: BoardSort = BoardSort.MANUAL,
)

class TodayViewModel(
    private val repository: HabitRepository,
) : ViewModel() {
    private val sort = MutableStateFlow(BoardSort.MANUAL)
    private val _state = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeActive(), repository.observeEntries(), sort) { habits, entries, order ->
                val rows = habits.map { habit ->
                    BoardHabit(
                        id = habit.id,
                        name = habit.name,
                        color = habit.color,
                        kind = habit.kind,
                        unit = habit.unit,
                        target = habit.target,
                        days = habit.days,
                        values = entries[habit.id].orEmpty(),
                    )
                }
                val ordered = when (order) {
                    BoardSort.MANUAL -> rows
                    BoardSort.NAME -> rows.sortedBy { it.name.lowercase() }
                }
                BoardState(loading = false, habits = ordered, sort = order)
            }.collect { _state.value = it }
        }
    }

    fun setSort(order: BoardSort) {
        sort.value = order
    }

    fun saveOrder(ids: List<Long>) {
        sort.value = BoardSort.MANUAL
        viewModelScope.launch {
            repository.reorder(ids)
        }
    }

    fun toggleYesNo(habit: BoardHabit, date: LocalDate) {
        val done = (habit.values[date] ?: 0.0) > 0.0
        viewModelScope.launch {
            repository.setEntry(habit.id, date, if (done) null else 1.0)
        }
    }

    fun setAmount(habitId: Long, date: LocalDate, value: Double?) {
        viewModelScope.launch {
            repository.setEntry(habitId, date, value)
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
