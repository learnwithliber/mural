package app.mural.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** AMOLED black, warm off-white text, one sand accent. */
object M {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF0F0F0E)
    val Surface2 = Color(0xFF191917)
    val Line = Color(0xFF2A2926)
    val Fg = Color(0xFFEDEAE3)
    val Muted = Color(0xFF8E8B84)
    val Accent = Color(0xFFE6D5B0)
    val AccentInk = Color(0xFF15130F)
    val Danger = Color(0xFFE58C7A)
}

@Composable
fun MuralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = M.Accent,
            onPrimary = M.AccentInk,
            secondary = M.Fg,
            onSecondary = M.AccentInk,
            background = M.Bg,
            onBackground = M.Fg,
            surface = M.Surface,
            onSurface = M.Fg,
            surfaceVariant = M.Surface2,
            onSurfaceVariant = M.Muted,
            outline = M.Line,
            outlineVariant = M.Line,
            secondaryContainer = M.Surface2,
            onSecondaryContainer = M.Fg,
            surfaceContainerLowest = M.Bg,
            surfaceContainerLow = M.Surface,
            surfaceContainer = M.Surface,
            surfaceContainerHigh = M.Surface2,
            surfaceContainerHighest = M.Surface2,
            error = M.Danger,
        ),
        content = content,
    )
}
