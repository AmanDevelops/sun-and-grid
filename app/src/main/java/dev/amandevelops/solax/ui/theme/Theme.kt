package dev.amandevelops.solax.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAppColors = staticCompositionLocalOf { LightColors }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SolaxTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.sun,
            onPrimary = colors.onSun,
            secondary = colors.grid,
            onSecondary = colors.white,
            background = colors.background,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.line,
            onSurfaceVariant = colors.mute,
            outline = colors.line,
            error = colors.bad,
            onError = colors.white,
        )
    } else {
        lightColorScheme(
            primary = colors.sun,
            onPrimary = colors.onSun,
            secondary = colors.grid,
            onSecondary = colors.white,
            background = colors.background,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.line,
            onSurfaceVariant = colors.mute,
            outline = colors.line,
            error = colors.bad,
            onError = colors.white,
        )
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
