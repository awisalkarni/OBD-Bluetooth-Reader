package awis.obd.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CarbonDark = Color(0xFF121417)
val SurfaceDark = Color(0xFF1E2228)
val SurfaceVariantDark = Color(0xFF2B303A)
val AccentCyan = Color(0xFF00E5FF)
val AccentOrange = Color(0xFFFF9100)
val AccentGreen = Color(0xFF00E676)
val AccentRed = Color(0xFFFF5252)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    secondary = AccentOrange,
    onSecondary = Color.Black,
    background = CarbonDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    secondary = Color(0xFFE65100),
    onSecondary = Color.White,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEAEFF5),
    error = Color(0xFFD32F2F)
)

@Composable
fun ObdReaderTheme(
    darkTheme: Boolean = true, // Default to sleek automotive dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
