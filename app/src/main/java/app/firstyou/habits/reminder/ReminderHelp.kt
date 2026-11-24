package app.firstyou.habits.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class ReminderSetupStatus(
    val notifications: String,
    val alarms: String,
    val battery: String,
    val alarmsAction: Boolean,
)

enum class HelpOpen {
    Target,
    Settings,
    Failed,
}

object ReminderHelp {
    fun status(context: Context): ReminderSetupStatus {
        val notificationsOn = notificationsAllowed(context)
        val alarms = alarmsLabel(context)
        return ReminderSetupStatus(
            notifications = if (notificationsOn) "Permitidas." else "Bloqueadas.",
            alarms = alarms,
            battery = batteryLabel(context),
            alarmsAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
    }

    fun openNotifications(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
        )
    }

    fun openAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return start(
            context,
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        )
    }

    fun openAppDetails(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        )
    }

    fun openAutoStart(context: Context): HelpOpen {
        val opened = openFirst(
            context,
            listOf(
                component("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
                component(
                    "com.transsion.phonemaster",
                    "com.transsion.phonemaster.startupmanager.StartupManagerActivity",
                ),
                Intent("com.mediatek.autobootcontroller.AUTO_BOOT").apply {
                    component = ComponentName(
                        "com.mediatek.autobootcontroller",
                        "com.mediatek.autobootcontroller.AutoBootAppManageActivity",
                    )
                },
            ),
        )
        if (opened) return HelpOpen.Target
        return if (start(context, Intent(Settings.ACTION_SETTINGS))) HelpOpen.Settings else HelpOpen.Failed
    }

    fun openPhoneMaster(context: Context): HelpOpen {
        val launch = context.packageManager.getLaunchIntentForPackage("com.transsion.phonemaster")
        if (launch != null && start(context, launch)) return HelpOpen.Target
        return if (start(context, Intent(Settings.ACTION_SETTINGS))) HelpOpen.Settings else HelpOpen.Failed
    }

    private fun notificationsAllowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun alarmsLabel(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return "Esta versión de Android no pide este permiso."
        }
        val alarms = context.getSystemService(AlarmManager::class.java)
        return if (alarms.canScheduleExactAlarms()) {
            "Permitidas."
        } else {
            "Sin permiso. El aviso puede llegar tarde."
        }
    }

    private fun batteryLabel(context: Context): String {
        val power = context.getSystemService(PowerManager::class.java)
        return if (power.isIgnoringBatteryOptimizations(context.packageName)) {
            "Android no limita la batería. En XOS igual revisa la actividad en segundo plano."
        } else {
            "Ábrela y revisa la batería de First Habits."
        }
    }

    private fun component(packageName: String, className: String): Intent {
        return Intent().setComponent(ComponentName(packageName, className))
    }

    private fun openFirst(context: Context, intents: List<Intent>): Boolean {
        return intents.any { intent ->
            intent.resolveActivity(context.packageManager) != null && start(context, intent)
        }
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: android.util.AndroidRuntimeException) {
            false
        }
    }
}
