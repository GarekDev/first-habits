package app.firstyou.habits.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("first_habits_settings")

class AppSettings(context: Context) {
    private val store = context.applicationContext.settingsStore

    val dark: Flow<Boolean> = store.data.map { it[DARK] ?: true }
    val accent: Flow<Int> = store.data.map { it[ACCENT] ?: DEFAULT_ACCENT }

    suspend fun setDark(value: Boolean) {
        store.edit { it[DARK] = value }
    }

    suspend fun setAccent(color: Int) {
        store.edit { it[ACCENT] = color }
    }

    companion object {
        const val DEFAULT_ACCENT = 0xFFD0BCFF.toInt()
        private val DARK = booleanPreferencesKey("dark")
        private val ACCENT = intPreferencesKey("accent")
    }
}
