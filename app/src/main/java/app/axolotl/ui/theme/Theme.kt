package app.axolotl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletAccent,
    onPrimaryContainer = SoftLavender,
    secondary = VioletSecondary,
    onSecondary = Color.Black,
    tertiary = VioletGlow,
    background = AnthraciteBg,
    onBackground = TextPrimary,
    surface = AnthraciteCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF262335),
    onSurfaceVariant = TextMuted,
    outline = AnthraciteCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark anthracite + violet design
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
