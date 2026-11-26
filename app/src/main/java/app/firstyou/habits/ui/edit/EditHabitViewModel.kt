package app.firstyou.habits.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.firstyou.habits.data.HabitColors
import app.firstyou.habits.data.HabitKind
import app.firstyou.habits.data.HabitRepository
import app.firstyou.habits.data.WeekDays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditHabitState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val color: Int = HabitColors.default,
    val days: Int = WeekDays.ALL,
    val reminderMinutes: Int? = null,
    val kind: HabitKind = HabitKind.YES_NO,
    val unit: String = "Minutos",
    val targetText: String = "30",
    val error: String? = null,
    val isNew: Boolean = true,
)

class EditHabitViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EditHabitState(isNew = habitId == 0L))
    val state: StateFlow<EditHabitState> = _state.asStateFlow()

    init {
        if (habitId == 0L) {
            _state.value = EditHabitState(loading = false, isNew = true)
        } else {
            viewModelScope.launch {
                val habit = repository.get(habitId)
                _state.value = if (habit == null) {
                    EditHabitState(loading = false, missing = true, isNew = false)
                } else {
                    EditHabitState(
                        loading = false,
                        name = habit.name,
                        color = habit.color,
                        days = habit.days,
                        reminderMinutes = habit.reminderMinutes,
                        kind = habit.kind,
                        unit = habit.unit ?: "Minutos",
                        targetText = habit.target?.let(::formatTarget) ?: "30",
                        isNew = false,
                    )
                }
            }
        }
    }

    fun onName(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun onColor(color: Int) {
        _state.update { it.copy(color = color) }
    }

    fun onToggleDay(index: Int) {
        _state.update { it.copy(days = WeekDays.toggle(it.days, index)) }
    }

    fun onReminder(minutes: Int?) {
        _state.update { it.copy(reminderMinutes = minutes) }
    }

    fun onKind(kind: HabitKind) {
        _state.update { it.copy(kind = kind, error = null) }
    }

    fun onUnit(value: String) {
        _state.update { it.copy(unit = value, error = null) }
    }

    fun onTarget(value: String) {
        _state.update { it.copy(targetText = value, error = null) }
    }

    fun save(onDone: () -> Unit) {
        val current = _state.value
        if (current.saving || current.name.isBlank()) return
        val target = current.targetText.replace(',', '.').toDoubleOrNull()
        if (current.kind == HabitKind.MEASURABLE && (current.unit.isBlank() || target == null || target <= 0.0)) {
            _state.update { it.copy(error = "Escribe la unidad y una meta mayor que cero.") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.save(
                id = if (current.isNew) null else habitId,
                name = current.name,
                color = current.color,
                days = current.days,
                reminderMinutes = current.reminderMinutes,
                kind = current.kind,
                unit = current.unit,
                target = target,
            )
            onDone()
        }
    }

    fun archive(onDone: () -> Unit) {
        if (habitId == 0L) return
        viewModelScope.launch {
            repository.archive(habitId)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (habitId == 0L) return
        viewModelScope.launch {
            repository.delete(habitId)
            onDone()
        }
    }

    companion object {
        fun factory(habitId: Long, repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditHabitViewModel(habitId, repository) as T
            }
        }
    }
}

private fun formatTarget(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
