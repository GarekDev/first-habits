package app.firstyou.habits.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.firstyou.habits.ui.archived.ArchivedScreen
import app.firstyou.habits.ui.backup.BackupScreen
import app.firstyou.habits.ui.detail.DetailScreen
import app.firstyou.habits.ui.edit.EditHabitScreen
import app.firstyou.habits.ui.help.ReminderHelpScreen
import app.firstyou.habits.ui.settings.SettingsScreen
import app.firstyou.habits.ui.today.TodayScreen

@Composable
fun HabitsNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "today") {
        composable("today") {
            TodayScreen(
                onNewHabit = { navController.navigate("edit/0") },
                onOpenHabit = { id -> navController.navigate("detail/$id") },
                onOpenArchived = { navController.navigate("archived") },
                onOpenReminderHelp = { navController.navigate("reminder-help") },
                onOpenBackup = { navController.navigate("backup") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("backup") {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable("reminder-help") {
            ReminderHelpScreen(onBack = { navController.popBackStack() })
        }
        composable("archived") {
            ArchivedScreen(onBack = { navController.popBackStack() })
        }
        composable("detail/{habitId}") { entry ->
            val habitId = entry.arguments?.getString("habitId")?.toLongOrNull() ?: 0L
            DetailScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("edit/$habitId") },
            )
        }
        composable("edit/{habitId}") { entry ->
            val habitId = entry.arguments?.getString("habitId")?.toLongOrNull() ?: 0L
            EditHabitScreen(
                habitId = habitId,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
