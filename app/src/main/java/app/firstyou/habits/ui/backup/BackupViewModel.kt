package app.firstyou.habits.ui.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.firstyou.habits.data.BackupFile
import app.firstyou.habits.data.HabitRepository
import app.firstyou.habits.data.decodeBackup
import app.firstyou.habits.data.encodeBackup
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(
    val busy: Boolean = false,
    val confirming: Boolean = false,
    val message: String? = null,
    val error: Boolean = false,
)

class BackupViewModel(private val repository: HabitRepository) : ViewModel() {
    private var pending: BackupFile? = null
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun exportTo(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupUiState(busy = true)
            val text = encodeBackup(repository.snapshot(LocalDateTime.now().toString()))
            val wrote = resolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
                true
            } ?: false
            _state.value = if (wrote) {
                BackupUiState(message = "Copia guardada.")
            } else {
                BackupUiState(message = "No se pudo guardar la copia.", error = true)
            }
        }
    }

    fun prepareImport(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupUiState(busy = true)
            val text = resolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
            val file = text?.let(::decodeBackup)
            if (file == null) {
                pending = null
                _state.value = BackupUiState(message = "No se pudo leer la copia.", error = true)
            } else {
                pending = file
                _state.value = BackupUiState(confirming = true)
            }
        }
    }

    fun confirmImport() {
        val file = pending ?: return
        viewModelScope.launch {
            _state.value = BackupUiState(busy = true)
            repository.replaceWith(file)
            pending = null
            _state.value = BackupUiState(message = "Copia restaurada.")
        }
    }

    fun cancelImport() {
        pending = null
        _state.value = BackupUiState()
    }

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BackupViewModel(repository) as T
            }
        }
    }
}
