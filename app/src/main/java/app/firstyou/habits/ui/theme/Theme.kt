package app.firstyou.habits.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1C1C1C)
private val LightBackground = Color(0xFFF3F3F3)
private val LightSurface = Color(0xFFFFFFFF)

@Composable
fun FirstHabitsTheme(
    dark: Boolean,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val onAccent = if (accent.luminance() > 0.62f) Color.Black else Color.White
    val scheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent,
            background = DarkBackground,
            onBackground = Color(0xFFF2F2F2),
            surface = DarkSurface,
            onSurface = Color(0xFFF2F2F2),
            onSurfaceVariant = Color(0xFFC8C8C8),
            surfaceContainer = Color(0xFF2A2A2A),
            surfaceContainerLow = Color(0xFF242424),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent,
            background = LightBackground,
            onBackground = Color(0xFF1C1B1A),
            surface = LightSurface,
            onSurface = Color(0xFF1C1B1A),
            onSurfaceVariant = Color(0xFF5C5854),
            surfaceContainer = Color(0xFFE7E7E7),
            surfaceContainerLow = Color(0xFFEFEFEF),
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content,
    )
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
