package app.firstyou.habits.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.firstyou.habits.data.Habit
import app.firstyou.habits.data.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchivedViewModel(private val repository: HabitRepository) : ViewModel() {
    private val _habits = MutableStateFlow<List<Habit>?>(null)
    val habits: StateFlow<List<Habit>?> = _habits.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeArchived().collect { _habits.value = it }
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            repository.restore(id)
        }
    }

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ArchivedViewModel(repository) as T
            }
        }
    }
}
